package com.github.jpmcodes.spawner.listeners;

import com.github.jpmcodes.spawner.JSpawnerPlugin;
import com.github.jpmcodes.spawner.data.models.CustomPlayer;
import com.github.jpmcodes.spawner.data.models.PlayerSpawnerModel;
import com.github.jpmcodes.spawner.data.models.SpawnerModel;
import com.github.jpmcodes.spawner.data.models.drop.DropModel;
import com.github.jpmcodes.spawner.utils.Messages;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


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
       // player.sendMessage(Messages.SPAWNER_PLACE_SUCCESS.getMessage().replace("{mob}", spawner.getType().name()));
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
            player.sendMessage(Messages.SPAWNER_NOT_OWNER.getMessage().replace("{owner}", ownerSpawner.getPlayer().getName()));
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
        //player.sendMessage(Messages.SPAWNER_BREAK_SUCCESS.getMessage());
        player.getInventory().addItem(spawner.getItem());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(EntityDeathEvent e) {
        LivingEntity entity = e.getEntity();
        int cont = getStackCount(entity);
        
        if (cont <= 0) {
            plugin.getLogger().warning("[Debug] Mob " + entity.getType() + " morreu mas o stack count foi 0 ou negativo.");
            return;
        }

        plugin.getLogger().info("[Debug] Mob " + entity.getType() + " morreu. Stack: " + cont);

        String spawnerId = entity.hasMetadata("spawner-id") && !entity.getMetadata("spawner-id").isEmpty() ? entity.getMetadata("spawner-id").get(0).asString() : null;
        SpawnerModel spawner = spawnerId != null ? plugin.getSpawnerCache().getByID(spawnerId) : null;
        
        if (spawner != null) {
            plugin.getLogger().info("[Debug] Spawner identificado: " + spawnerId + " (Custom drops: " + (spawner.getDrops() != null ? spawner.getDrops().size() : 0) + ")");
        } else if (spawnerId != null) {
            plugin.getLogger().warning("[Debug] Mob tinha ID de spawner (" + spawnerId + ") mas o spawner nao foi encontrado no cache!");
        }

        if (plugin.getConfigs().getBoolean("stack-mobs.kill-all") && entity.getKiller() != null && !entity.getKiller().isSneaking()) {
            plugin.getLogger().info("[Debug] Kill-all ativado para " + entity.getKiller().getName());
            e.setDroppedExp(e.getDroppedExp() * cont);

            if (spawner != null && spawner.getDrops() != null && !spawner.getDrops().isEmpty()) {
                e.getDrops().clear();
                int totalItemsGenerated = 0;
                for (int i = 0; i < cont; i++) {
                    for (DropModel dropModel : spawner.getDrops()) {
                        if (Math.random() <= dropModel.getChance()) {
                            int amount = dropModel.getMinAmount();
                            if (dropModel.getMaxAmount() > dropModel.getMinAmount()) {
                                amount += (int) (Math.random() * (dropModel.getMaxAmount() - dropModel.getMinAmount() + 1));
                            }
                            if (amount > 0) {
                                ItemStack item = dropModel.getItem().clone();
                                item.setAmount(amount);
                                addDropSafely(e.getDrops(), item);
                                totalItemsGenerated += amount;
                            }
                        }
                    }
                }
                plugin.getLogger().info("[Debug] Custom drops gerados: " + totalItemsGenerated + " itens totais.");
            } else {
                List<ItemStack> vanillaDrops = new ArrayList<>(e.getDrops());
                e.getDrops().clear();
                plugin.getLogger().info("[Debug] Multiplicando drops vanilla. Originais: " + vanillaDrops.size());
                for (ItemStack drop : vanillaDrops) {
                    if (drop == null || drop.getType() == Material.AIR) continue;
                    
                    int totalAmount = drop.getAmount() * cont;
                    plugin.getLogger().info("[Debug]  - " + drop.getType() + " x " + drop.getAmount() + " -> Total: " + totalAmount);
                    
                    addDropSafely(e.getDrops(), drop, totalAmount);
                }
            }
        } else {
            plugin.getLogger().info("[Debug] Kill-all desativado ou condicao nao atingida (Sneaking ou sem Killer).");
            if (spawner != null && spawner.getDrops() != null && !spawner.getDrops().isEmpty()) {
                e.getDrops().clear();
                for (DropModel dropModel : spawner.getDrops()) {
                    if (Math.random() <= dropModel.getChance()) {
                        int amount = dropModel.getMinAmount();
                        if (dropModel.getMaxAmount() > dropModel.getMinAmount()) {
                            amount += (int) (Math.random() * (dropModel.getMaxAmount() - dropModel.getMinAmount() + 1));
                        }
                        if (amount > 0) {
                            ItemStack item = dropModel.getItem().clone();
                            item.setAmount(amount);
                            addDropSafely(e.getDrops(), item);
                        }
                    }
                }
            }

            if (cont > 1) {
                int newCont = cont - 1;
                LivingEntity spawned = (LivingEntity) entity.getWorld().spawnEntity(entity.getLocation(), e.getEntityType());
                spawned.setCustomName(plugin.getConfigs().getString("stack-mobs.display-name")
                        .replace("&", "§")
                        .replace("{mob}", entity.getType().name())
                        .replace("{count}", String.valueOf(newCont)));
                spawned.setCustomNameVisible(true);
                spawned.setMetadata("stack-spawner", new FixedMetadataValue(plugin, newCont));
                if (spawnerId != null) {
                    spawned.setMetadata("spawner-id", new FixedMetadataValue(plugin, spawnerId));
                }
                plugin.getLogger().info("[Debug] Mob restante spawnado. Novo stack: " + newCont);
            }
        }
        plugin.getLogger().info("[Debug] Lista final de drops para o evento: " + e.getDrops().size() + " stacks de itens.");
    }

    private int getStackCount(LivingEntity entity) {
        if (entity.hasMetadata("stack-spawner")) {
            int count = entity.getMetadata("stack-spawner").get(0).asInt();
            plugin.getLogger().info("[Debug] Stack count recuperado via Metadata: " + count);
            return count;
        }

        String name = entity.getCustomName();
        if (name != null && !name.isEmpty()) {
            try {
                String stripped = org.bukkit.ChatColor.stripColor(name);
                if (stripped.contains("x")) {
                    String countPart = stripped.split("x")[0];
                    int count = Integer.parseInt(countPart.trim());
                    plugin.getLogger().info("[Debug] Stack count recuperado via Name: " + count);
                    return count;
                }
            } catch (Exception ignored) {
            }
        }
        plugin.getLogger().warning("[Debug] Nao foi possivel recuperar o stack count para " + entity.getType());
        return 0;
    }

    private void addDropSafely(List<ItemStack> drops, ItemStack item) {
        int amount = item.getAmount();
        int maxStack = item.getType().getMaxStackSize();
        if (maxStack <= 0) maxStack = 64;

        while (amount > 0) {
            ItemStack clone = item.clone();
            clone.setAmount(Math.min(amount, maxStack));
            drops.add(clone);
            amount -= clone.getAmount();
        }
    }

    private void addDropSafely(List<ItemStack> drops, ItemStack original, int totalAmount) {
        int maxStack = original.getType().getMaxStackSize();
        if (maxStack <= 0) maxStack = 64;

        while (totalAmount > 0) {
            ItemStack clone = original.clone();
            clone.setAmount(Math.min(totalAmount, maxStack));
            drops.add(clone);
            totalAmount -= clone.getAmount();
        }
    }
}