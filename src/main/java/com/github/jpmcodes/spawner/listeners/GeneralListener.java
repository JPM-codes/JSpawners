package com.github.jpmcodes.spawner.listeners;

import com.github.jpmcodes.spawner.JSpawnerPlugin;
import com.github.jpmcodes.spawner.data.models.CustomPlayer;
import com.github.jpmcodes.spawner.data.models.PlayerSpawnerModel;
import com.github.jpmcodes.spawner.data.models.SpawnerModel;
import com.github.jpmcodes.spawner.utils.Messages;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedList;

import static com.github.jpmcodes.spawner.tasks.SpawnerTask.clearStack;

@RequiredArgsConstructor
public class GeneralListener implements Listener {

    private final JSpawnerPlugin plugin;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        CustomPlayer customPlayer = plugin.getCustomPlayerCache().getByUUID(player.getUniqueId());
        if (customPlayer != null) return;

        plugin.getCustomPlayerCache().addCachedElements(
                new CustomPlayer(
                        player.getUniqueId(),
                        player.getName()
                )
        );

    }

    @EventHandler
    public void onSpawnerPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        ItemStack item = player.getInventory().getItemInHand();
        Block block = e.getBlockPlaced();

        SpawnerModel spawner = plugin.getSpawnerCache().getByItem(item);
        if (spawner == null) return;

        PlayerSpawnerModel playerSpawner = plugin.getPlayerSpawnerCache().getByPlayerUUID(player.getUniqueId());
        if (playerSpawner == null) {
            playerSpawner = new PlayerSpawnerModel(
                    plugin.getCustomPlayerCache().getByUUID(player.getUniqueId()),
                    new LinkedList<>()
            );
            plugin.getPlayerSpawnerCache().addCachedElements(playerSpawner);
        }

        boolean limitPerPlayer = plugin.getConfigs().getConfig().getBoolean("spawner-limit.enable");
        int maxSpawners = plugin.getConfigs().getConfig().getInt("spawner-limit.max");
        if (limitPerPlayer) {
            if (playerSpawner.getSpawners().size() >= maxSpawners) {
                player.sendMessage(Messages.SPAWNER_LIMIT_REACHED.getMessage()
                        .replace("{limit}", String.valueOf(maxSpawners)));
                e.setCancelled(true);
                return;
            }
        }

        // Clone primeiro, depois define a localização

        if (item.getType().equals(Material.MOB_SPAWNER)) {
            //definir o tipo do mob para o spawner
            CreatureSpawner creatureSpawner = (org.bukkit.block.CreatureSpawner) block.getState();
            creatureSpawner.setSpawnedType(spawner.getType());
        }

        SpawnerModel placedSpawner = spawner.clone();
        placedSpawner.setLocation(block.getLocation());
        playerSpawner.add(placedSpawner);

        item.setAmount(item.getAmount() - 1);
        player.sendMessage(Messages.SPAWNER_PLACE_SUCCESS.getMessage()
                .replace("{mob}", spawner.getType().name()));
    }

    @EventHandler
    public void onSpawnerBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();
        ItemStack item = player.getItemInHand();
        Block block = e.getBlock();

        // Busca em TODOS os jogadores se o bloco é um spawner registrado
        PlayerSpawnerModel ownerSpawner = plugin.getPlayerSpawnerCache().getCachedElements().stream()
                .filter(ps -> ps.getSpawners().stream()
                        .anyMatch(s -> s.getLocation().equals(block.getLocation())))
                .findFirst()
                .orElse(null);

        // O bloco não é um spawner registrado, ignora
        if (ownerSpawner == null) return;

        // Verifica se o jogador que está quebrando é o dono
        if (!ownerSpawner.getPlayer().getUuid().equals(player.getUniqueId())) {
            player.sendMessage(Messages.SPAWNER_NOT_OWNER.getMessage());
            e.setCancelled(true);
            return;
        }

        SpawnerModel spawner = ownerSpawner.getSpawners().stream()
                .filter(s -> s.getLocation().equals(block.getLocation()))
                .findFirst()
                .orElse(null);

        if (spawner == null) return;

        // Verifica se a config pede que o spawner seja quebrado com silk touch
        if (plugin.getConfigs().getBoolean("break-spawner-with-silk-touch")) {
            if (!item.containsEnchantment(org.bukkit.enchantments.Enchantment.SILK_TOUCH)) {
                player.sendMessage(Messages.SPAWNER_SILK_TOUCH_REQUIRED.getMessage());
                e.setCancelled(true);
                return;
            }
        }

        ownerSpawner.getSpawners().remove(spawner);
        player.sendMessage(Messages.SPAWNER_BREAK_SUCCESS.getMessage());
        player.getInventory().addItem(spawner.getItem());
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        clearStack(e.getEntity());
    }
}
