package com.github.jpmcodes.spawner.listeners;

import com.github.jpmcodes.autoitem.AutoItemPlugin;
import com.github.jpmcodes.spawner.JSpawnerPlugin;
import com.github.jpmcodes.spawner.data.models.CustomPlayer;
import com.github.jpmcodes.spawner.data.models.PlayerSpawnerModel;
import com.github.jpmcodes.spawner.data.models.SpawnerModel;
import com.github.jpmcodes.spawner.data.models.drop.DropModel;
import com.github.jpmcodes.spawner.utils.Debug;
import com.github.jpmcodes.spawner.utils.LocationUtils;
import com.github.jpmcodes.spawner.utils.Messages;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

@RequiredArgsConstructor
public class GeneralListener implements Listener {

    private final JSpawnerPlugin plugin;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        CustomPlayer customPlayer = plugin.getCustomPlayerCache().getByUUID(player.getUniqueId());

        if (customPlayer == null) {
            customPlayer = new CustomPlayer(
                    player.getUniqueId(),
                    player.getName()
            );
            plugin.getCustomPlayerCache().addCachedElements(customPlayer);
            plugin.getCustomPlayerStorage().save(customPlayer);
        }

        // Garante que o PlayerSpawnerModel existe no cache para o jogador
        PlayerSpawnerModel playerSpawner = plugin.getPlayerSpawnerCache().getByPlayerUUID(player.getUniqueId());
        if (playerSpawner == null) {
            playerSpawner = new PlayerSpawnerModel(customPlayer, new LinkedList<>());
            plugin.getPlayerSpawnerCache().addCachedElements(playerSpawner);
        }

        // Carrega os spawners do banco de dados para o cache se necessário (on-demand)
        plugin.getPlayerSpawnerStorage().loadForPlayer(customPlayer);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        ItemStack item = player.getInventory().getItemInHand();
        Block block = e.getClickedBlock();

        if (e.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        // Se o item na mão for um ovo de mob e o bloco não for um spawner, bloqueia a interação
        if (item.getType() == Material.MONSTER_EGG && !block.getType().equals(Material.MOB_SPAWNER)) {
            e.setCancelled(true);
            e.setUseInteractedBlock(Event.Result.DENY);
            e.setUseItemInHand(Event.Result.DENY);
            player.sendMessage("§cVocê só pode usar ovos de mob em spawners!");
            return;
        }

        if (block.getLocation().getWorld().getName().equalsIgnoreCase("Mundo1")) {
            if (item.getType() == Material.MONSTER_EGG) {
                e.setCancelled(true);
                short data = item.getData().getData();

                EntityType type = EntityType.fromId(data);
                if (type == null) return;

                if (block.getState() instanceof CreatureSpawner) {
                    CreatureSpawner creatureSpawner = (CreatureSpawner) block.getState();
                    creatureSpawner.setSpawnedType(type);
                    creatureSpawner.update();
                }

                // Remover 1 item do ovo do mob (usando o ID do mob como durabilidade no 1.5.2)
                if (player.getItemInHand().getAmount() > 1) {
                    player.getItemInHand().setAmount(item.getAmount() - 1);
                } else {
                    player.setItemInHand(new ItemStack(Material.AIR));
                }
                player.updateInventory();
            }
            return;
        }

        SpawnerModel spawnerTemplate = plugin.getSpawnerCache().getByEgg(item);
        if (spawnerTemplate == null) return;

        e.setCancelled(true);

        // Busca se o spawner já é registrado por ALGUÉM
        PlayerSpawnerModel ownerModel = plugin.getPlayerSpawnerCache().getCachedElements().stream()
                .filter(ps -> ps.getSpawners().stream()
                        .anyMatch(s -> s.getLocation().equals(block.getLocation())))
                .findFirst()
                .orElse(null);

        if (ownerModel != null && !ownerModel.getPlayer().getUuid().equals(player.getUniqueId()) && !player.isOp()) {
            player.sendMessage(Messages.SPAWNER_NOT_OWNER.getMessage().replace("{owner}", ownerModel.getPlayer().getName()));
            return;
        }

        PlayerSpawnerModel playerSpawner = plugin.getPlayerSpawnerCache().getByPlayerUUID(player.getUniqueId());
        if (playerSpawner == null) {
            CustomPlayer customPlayer = plugin.getCustomPlayerCache().getByUUID(player.getUniqueId());
            if (customPlayer == null) {
                customPlayer = new CustomPlayer(player.getUniqueId(), player.getName());
                plugin.getCustomPlayerCache().addCachedElements(customPlayer);
                plugin.getCustomPlayerStorage().save(customPlayer);
            }
            playerSpawner = new PlayerSpawnerModel(
                    customPlayer,
                    new LinkedList<>()
            );
            plugin.getPlayerSpawnerCache().addCachedElements(playerSpawner);
        }

        //definir o tipo do mob para o spawner
        if (block.getState() instanceof CreatureSpawner) {
            CreatureSpawner creatureSpawner = (CreatureSpawner) block.getState();
            creatureSpawner.setSpawnedType(spawnerTemplate.getType());
            creatureSpawner.update();
        }

        // Se já era dono ou se o spawner não era registrado, garante que está na lista do jogador sem duplicatas
        playerSpawner.getSpawners().removeIf(s -> s.getLocation().equals(block.getLocation()));

        SpawnerModel placedSpawner = spawnerTemplate.clone();
        placedSpawner.setLocation(block.getLocation());
        playerSpawner.add(placedSpawner);

        // Remover 1 item do ovo do mob (usando o ID do mob como durabilidade no 1.5.2)
        if (player.getItemInHand().getAmount() > 1) {
            player.getItemInHand().setAmount(item.getAmount() - 1);
        } else {
            player.setItemInHand(new ItemStack(Material.AIR));
        }
        player.updateInventory();


        plugin.getPlayerSpawnerStorage().save(playerSpawner, placedSpawner);
    }

    @EventHandler
    public void onSpawnerBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();
        Block block = e.getBlock();

        // Busca em TODOS os jogadores se o bloco é um spawner registrado
        PlayerSpawnerModel ownerSpawner = plugin.getPlayerSpawnerCache().getCachedElements().stream()
                .filter(ps -> ps.getSpawners().stream()
                        .anyMatch(s -> s.getLocation().equals(block.getLocation())))
                .findFirst()
                .orElse(null);

        // O bloco não é um spawner registrado, verificar se é um spawner "solto" (sem dono) e permitir quebrar normalmente
        if (ownerSpawner == null) {
            if (block.getType() == Material.MOB_SPAWNER) {
                ItemStack tool = player.getItemInHand();
                if (tool != null && tool.containsEnchantment(org.bukkit.enchantments.Enchantment.SILK_TOUCH)) {
                    // Entrega o bloco do Spawner
                    player.getInventory().addItem(new ItemStack(Material.MOB_SPAWNER));
                    block.setType(Material.AIR);
                }
                return;
            }
            return;
        }

        // Verifica se o jogador que está quebrando é o dono
        if (!ownerSpawner.getPlayer().getUuid().equals(player.getUniqueId()) && !player.isOp()) {
            player.sendMessage(Messages.SPAWNER_NOT_OWNER.getMessage().replace("{owner}", ownerSpawner.getPlayer().getName()));
            e.setCancelled(true);
            return;
        }

        SpawnerModel spawner = ownerSpawner.getSpawners().stream()
                .filter(s -> s.getLocation().equals(block.getLocation()))
                .findFirst()
                .orElse(null);

        if (spawner == null) return;
        ownerSpawner.getSpawners().remove(spawner);
        plugin.getPlayerSpawnerStorage().delete(ownerSpawner, spawner);
        
        // Lógica de Silk Touch
        ItemStack tool = player.getItemInHand();
        if (tool != null && tool.containsEnchantment(org.bukkit.enchantments.Enchantment.SILK_TOUCH)) {
            // Entrega o bloco do Spawner
            player.getInventory().addItem(new ItemStack(Material.MOB_SPAWNER));
            
            // Entrega o ovo do mob (usando o ID do mob como durabilidade no 1.5.2)
            player.getInventory().addItem(new ItemStack(Material.MONSTER_EGG, 1, spawner.getType().getTypeId()));
            
            player.sendMessage(Messages.SPAWNER_BREAK_SUCCESS.getMessage());
            
            // Remove o bloco sem dropar nada extra (como XP)
            block.setType(Material.AIR);
        }
    }

    @EventHandler
    public void onMobSpawn(SpawnerSpawnEvent e) {
        // Se o nome do mundo for MUNDO1, irá funcionar apenas os spawners normal dos mundos
        if (e.getLocation().getWorld().getName().equalsIgnoreCase("Mundo1")) {

            Entity entity = e.getEntity();
            Location location = e.getLocation();

            LivingEntity stackedTarget = LocationUtils.getNearbyLivingEntity(location, plugin.getConfigs().getInt("stack-mobs.stack-radius"), entity.getType());

            boolean enableStack = plugin.getConfigs().getBoolean("stack-mobs.enable");

            if (enableStack) {
                if (stackedTarget != null) {

                    int newAmount = stackedTarget.hasMetadata("stack-spawner")
                            ? stackedTarget.getMetadata("stack-spawner").get(0).asInt()
                            : 1;

                    if (newAmount > plugin.getConfigs().getInt("stack-mobs.max-stack-size")) {
                        newAmount = plugin.getConfigs().getInt("stack-mobs.max-stack-size");
                    }

                    stackedTarget.setCustomName(plugin.getConfigs().getString("stack-mobs.display-name")
                            .replace("&", "§")
                            .replace("{count}", String.valueOf(newAmount))
                            .replace("{mob}", entity.getType().name()));
                    stackedTarget.setCustomNameVisible(true);


                    stackedTarget.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 100));
                    stackedTarget.setVelocity(new Vector(0, 0, 0));
                    LocationUtils.setNoAI(stackedTarget);
                    stackedTarget.setMetadata("mob_spawner", new FixedMetadataValue(plugin, true));
                    stackedTarget.setMetadata("stack-spawner", new FixedMetadataValue(plugin, newAmount));

                } else {
                    LivingEntity livingEntity = (LivingEntity) entity;
                    livingEntity.setCustomName(plugin.getConfigs().getString("stack-mobs.display-name")
                            .replace("&", "§")
                            .replace("{count}", String.valueOf(1))
                            .replace("{mob}", entity.getType().name()));
                    livingEntity.setCustomNameVisible(true);

                    livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 100));
                    entity.setVelocity(new Vector(0, 0, 0));
                    LocationUtils.setNoAI(entity);
                    entity.setMetadata("stack-spawner", new FixedMetadataValue(plugin, 1));
                    entity.setMetadata("mob_spawner", new FixedMetadataValue(plugin, true));
                }
            } else {
                for (int i = 0; i < 1; i++) {
                    entity.setVelocity(new Vector(0, 0, 0));
                    LocationUtils.setNoAI(entity);
                    entity.setMetadata("mob_spawner", new FixedMetadataValue(plugin, true));
                }
            }
            return;
        }

        e.setCancelled(true);
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(EntityDeathEvent e) {
        LivingEntity entity = e.getEntity();
        int cont = getStackCount(entity);

        if (cont <= 0) {
            Debug.warning("[Debug] Mob " + entity.getType() + " morreu mas o stack count foi 0 ou negativo.");
            return;
        }

        Debug.info("[Debug] Mob " + entity.getType() + " morreu. Stack: " + cont);

        String spawnerId = entity.hasMetadata("spawner-id") && !entity.getMetadata("spawner-id").isEmpty() ? entity.getMetadata("spawner-id").get(0).asString() : null;
        SpawnerModel spawner = spawnerId != null ? plugin.getSpawnerCache().getByID(spawnerId) : null;

        if (spawner != null) {
            Debug.info("[Debug] Spawner identificado: " + spawnerId + " (Custom drops: " + (spawner.getDrops() != null ? spawner.getDrops().size() : 0) + ")");
        } else if (spawnerId != null) {
            Debug.warning("[Debug] Mob tinha ID de spawner (" + spawnerId + ") mas o spawner nao foi encontrado no cache!");
        }

        List<ItemStack> finalDrops = new ArrayList<>();

        List<ItemStack> eventDrops = e.getDrops();

        if (plugin.getConfigs().getBoolean("stack-mobs.kill-all") && entity.getKiller() != null && !entity.getKiller().isSneaking()) {
            Player killer = entity.getKiller();
            Debug.info("[Debug] Kill-all ativado para " + killer.getName());
            e.setDroppedExp(e.getDroppedExp() * cont);

            // Cálculo do nível de Pilhagem (Looting)
            int lootingLevel = 0;
            if (killer.getItemInHand() != null) {
                lootingLevel = killer.getItemInHand().getEnchantmentLevel(org.bukkit.enchantments.Enchantment.LOOT_BONUS_MOBS);
            }
            if (lootingLevel > 0) {
                Debug.info("[Debug] Pilhagem identificada: Nivel " + lootingLevel);
            }

            if (spawner != null && spawner.getDrops() != null && !spawner.getDrops().isEmpty()) {
                int totalItemsGenerated = 0;
                for (int i = 0; i < cont; i++) {
                    for (DropModel dropModel : spawner.getDrops()) {
                        if (Math.random() <= dropModel.getChance()) {
                            int amount = dropModel.getMinAmount();
                            if (dropModel.getMaxAmount() > dropModel.getMinAmount()) {
                                amount += (int) (Math.random() * (dropModel.getMaxAmount() - dropModel.getMinAmount() + 1));
                            }

                            // Aplicar bônus de Pilhagem nos drops customizados
                            // Logica: Cada nível de pilhagem pode adicionar entre 0 e 1 item extra (simulando vanilla)
                            if (lootingLevel > 0 && amount > 0) {
                                amount += (int) (Math.random() * (lootingLevel + 1));
                            }

                            if (amount > 0) {
                                ItemStack item = dropModel.getItem().clone();
                                item.setAmount(amount);
                                addDropSafely(finalDrops, item);
                                totalItemsGenerated += amount;
                            }
                        }
                    }
                }
                Debug.info("[Debug] Custom drops gerados: " + totalItemsGenerated + " itens totais (Pilhagem inclusa).");
                eventDrops.clear();
                eventDrops.addAll(finalDrops);
            } else {
                List<ItemStack> vanillaDrops = new ArrayList<>(eventDrops);
                eventDrops.clear();
                Debug.info("[Debug] Multiplicando drops vanilla. Originais: " + vanillaDrops.size());
                for (ItemStack drop : vanillaDrops) {
                    if (drop == null || drop.getType() == Material.AIR) continue;

                    int totalAmount = drop.getAmount() * cont;
                    Debug.info("[Debug]  - " + drop.getType() + " x " + drop.getAmount() + " -> Total: " + totalAmount);

                    addDropSafely(finalDrops, drop, totalAmount);
                }
                eventDrops.addAll(finalDrops);
            }
        } else {
            Debug.info("[Debug] Kill-all desativado ou condicao nao atingida (Sneaking ou sem Killer).");

            // Cálculo do nível de Pilhagem (Looting) para morte individual
            int lootingLevel = 0;
            if (entity.getKiller() != null && entity.getKiller().getItemInHand() != null) {
                lootingLevel = entity.getKiller().getItemInHand().getEnchantmentLevel(org.bukkit.enchantments.Enchantment.LOOT_BONUS_MOBS);
            }

            if (spawner != null && spawner.getDrops() != null && !spawner.getDrops().isEmpty()) {
                eventDrops.clear();
                for (DropModel dropModel : spawner.getDrops()) {
                    if (Math.random() <= dropModel.getChance()) {
                        int amount = dropModel.getMinAmount();
                        if (dropModel.getMaxAmount() > dropModel.getMinAmount()) {
                            amount += (int) (Math.random() * (dropModel.getMaxAmount() - dropModel.getMinAmount() + 1));
                        }

                        // Pilhagem para drop customizado individual
                        if (lootingLevel > 0 && amount > 0) {
                            amount += (int) (Math.random() * (lootingLevel + 1));
                        }

                        if (amount > 0) {
                            ItemStack item = dropModel.getItem().clone();
                            item.setAmount(amount);
                            addDropSafely(eventDrops, item);
                        }
                    }
                }
            }

            if (cont > 1) {
                int newCont = cont - 1;
                LivingEntity spawned = (LivingEntity) entity.getWorld().spawnEntity(entity.getLocation(), e.getEntityType());
                spawned.setNoDamageTicks(0);
                spawned.setCustomName(plugin.getConfigs().getString("stack-mobs.display-name")
                        .replace("&", "§")
                        .replace("{mob}", entity.getType().name())
                        .replace("{count}", String.valueOf(newCont)));
                spawned.setCustomNameVisible(true);
                spawned.setMetadata("stack-spawner", new FixedMetadataValue(plugin, newCont));
                spawned.setMetadata("mob_spawner", new FixedMetadataValue(plugin, true));
                if (spawnerId != null) {
                    spawned.setMetadata("spawner-id", new FixedMetadataValue(plugin, spawnerId));
                }
                
                // Aplicar NoAI e remover velocidade no novo mob do stack
                LocationUtils.setNoAI(spawned);
                spawned.setVelocity(new Vector(0, 0, 0));
                
                Debug.info("[Debug] Mob restante spawnado. Novo stack: " + newCont);
            }
        }

        int totalItemsInList = 0;
        for (ItemStack drop : e.getDrops()) {
            if (drop != null) totalItemsInList += drop.getAmount();
        }
        Debug.info("[Debug] Lista final de drops para o evento: " + e.getDrops().size() + " stacks (" + totalItemsInList + " itens totais).");

        // Integração com JAutoItem para enviar itens direto para o inventário
        if (entity.getKiller() != null && !e.getDrops().isEmpty()) {
            Player killer = entity.getKiller();
            List<ItemStack> dropsToGive = new ArrayList<>(e.getDrops());

            // Limpa os drops do chão para não duplicar
            e.getDrops().clear();

            // Entrega via API do JAutoItem
            AutoItemPlugin.getApi().giveItems(killer, dropsToGive, entity.getLocation());
            Debug.info("[Debug] Itens enviados para o inventário de " + killer.getName() + " via JAutoItem.");
        }
    }

    private int getStackCount(LivingEntity entity) {
        if (entity.hasMetadata("stack-spawner")) {
            int count = entity.getMetadata("stack-spawner").get(0).asInt();
            Debug.info("[Debug] Stack count recuperado via Metadata: " + count);
            return count;
        }

        String name = entity.getCustomName();
        if (name != null && !name.isEmpty()) {
            try {
                String stripped = org.bukkit.ChatColor.stripColor(name);
                if (stripped.contains("x")) {
                    String countPart = stripped.split("x")[0];
                    int count = Integer.parseInt(countPart.trim());
                    Debug.info("[Debug] Stack count recuperado via Name: " + count);
                    return count;
                }
            } catch (Exception ignored) {
            }
        }
        Debug.warning("[Debug] Nao foi possivel recuperar o stack count para " + entity.getType());
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