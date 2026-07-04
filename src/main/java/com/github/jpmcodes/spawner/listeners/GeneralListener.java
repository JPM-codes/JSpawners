package com.github.jpmcodes.spawner.listeners;

import com.github.jpmcodes.spawner.JSpawnerPlugin;
import com.github.jpmcodes.spawner.data.models.CustomPlayer;
import com.github.jpmcodes.spawner.data.models.PlayerSpawnerModel;
import com.github.jpmcodes.spawner.data.models.SpawnerModel;
import com.github.jpmcodes.spawner.data.models.drop.DropModel;
import com.github.jpmcodes.spawner.utils.LocationUtils;
import com.github.jpmcodes.spawner.utils.Messages;

import java.util.*;

import com.gmail.nossr50.api.ExperienceAPI;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.SkillType;
import com.gmail.nossr50.datatypes.skills.ToolType;
import com.gmail.nossr50.skills.archery.ArcheryManager;
import com.gmail.nossr50.util.player.UserManager;
import com.gmail.nossr50.util.skills.CombatUtils;
import lombok.Generated;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class GeneralListener implements Listener {
    private static final String MANUAL_STACK_DEATH = "manual-stack-death";

    private static final Map<String, Long> HIT_COOLDOWN = new HashMap<>();
    private static final long HIT_COOLDOWN_MS = 0; // 0.5s

    private final JSpawnerPlugin plugin;

    @Generated
    public GeneralListener(JSpawnerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        CustomPlayer customPlayer = this.plugin.getCustomPlayerCache().getByUUID(player.getUniqueId());

        if (customPlayer == null) {

            customPlayer = new CustomPlayer(player.getUniqueId(), player.getName());

            this.plugin.getCustomPlayerCache().addCachedElements(customPlayer);
            this.plugin.getCustomPlayerStorage().save(customPlayer);
        }

        PlayerSpawnerModel playerSpawner = this.plugin.getPlayerSpawnerCache().getByPlayerUUID(player.getUniqueId());
        if (playerSpawner == null) {
            playerSpawner = new PlayerSpawnerModel(customPlayer, new LinkedList<>());
            this.plugin.getPlayerSpawnerCache().addCachedElements(playerSpawner);
            this.plugin.getPlayerSpawnerStorage().loadForPlayer(customPlayer);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();

        CustomPlayer customPlayer = this.plugin.getCustomPlayerCache().getByUUID(player.getUniqueId());
        if (customPlayer != null) {
            this.plugin.getCustomPlayerCache().getCachedElements().remove(customPlayer);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        ItemStack item = player.getInventory().getItemInHand();
        Block block = e.getClickedBlock();
        if (block == null) return;
        Location location = block.getLocation();

        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (item.getType() == Material.MONSTER_EGG && !block.getType().equals(Material.MOB_SPAWNER)) {
            e.setCancelled(true);
            e.setUseInteractedBlock(Event.Result.DENY);
            e.setUseItemInHand(Event.Result.DENY);
            player.sendMessage("§cVocê só pode usar ovos de mob em spawners!");
            return;
        }

        if (location.getWorld().getName().equalsIgnoreCase("spawn1")) {
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

                if (player.getItemInHand().getAmount() > 1) {
                    player.getItemInHand().setAmount(item.getAmount() - 1);
                } else {
                    player.setItemInHand(new ItemStack(Material.AIR));
                }
            }

            return;
        }

        SpawnerModel spawnerTemplate = this.plugin.getSpawnerCache().getByEgg(item);
        if (spawnerTemplate == null) return;

        // Restrição Golem
        if (spawnerTemplate.getType() == EntityType.IRON_GOLEM && !player.hasPermission("jpspawner.golem.bypass") && !player.isOp()) {
            player.sendMessage("§cVocê não pode colocar spawners de Golem!");
            e.setCancelled(true);
            return;
        }

        e.setCancelled(true);


        PlayerSpawnerModel ownerModel = this.plugin.getPlayerSpawnerCache().getCachedElements().stream().filter(ps -> ps.getSpawners().stream().anyMatch(s -> s.getLocation().equals(location))).findFirst().orElse(null);

        if (ownerModel != null && !ownerModel.getPlayer().getUuid().equals(player.getUniqueId()) && !player.isOp()) {
            player.sendMessage(Messages.SPAWNER_NOT_OWNER.getMessage().replace("{owner}", ownerModel.getPlayer().getName()));

            return;
        }
        PlayerSpawnerModel playerSpawner = this.plugin.getPlayerSpawnerCache().getByPlayerUUID(player.getUniqueId());
        if (playerSpawner == null) {
            CustomPlayer customPlayer = this.plugin.getCustomPlayerCache().getByUUID(player.getUniqueId());
            if (customPlayer == null) {
                customPlayer = new CustomPlayer(player.getUniqueId(), player.getName());
                this.plugin.getCustomPlayerCache().addCachedElements(customPlayer);
                this.plugin.getCustomPlayerStorage().save(customPlayer);
            }
            playerSpawner = new PlayerSpawnerModel(customPlayer, new LinkedList<>());


            this.plugin.getPlayerSpawnerCache().addCachedElements(playerSpawner);
        }

        if (block.getState() instanceof CreatureSpawner) {
            CreatureSpawner creatureSpawner = (CreatureSpawner) block.getState();

            if (creatureSpawner.getCreatureTypeName().equals(spawnerTemplate.getType().name())) {
                e.setCancelled(true);
                return;
            }

            creatureSpawner.setSpawnedType(spawnerTemplate.getType());
            creatureSpawner.update();
        }

        playerSpawner.getSpawners().removeIf(s -> s.getLocation().equals(location));

        SpawnerModel placedSpawner = spawnerTemplate.clone();
        placedSpawner.setLocation(location);
        placedSpawner.setOwnerUuid(player.getUniqueId());
        playerSpawner.add(placedSpawner);
        this.plugin.getPlayerSpawnerCache().addSpawnerToChunk(placedSpawner);

        if (player.getItemInHand().getAmount() > 1) {
            player.getItemInHand().setAmount(item.getAmount() - 1);
        } else {
            player.setItemInHand(new ItemStack(Material.AIR));
        }
        this.plugin.getPlayerSpawnerStorage().save(playerSpawner, placedSpawner);
    }

    @EventHandler
    public void onSpawnerBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();
        Block block = e.getBlock();
        Location location = block.getLocation();

        PlayerSpawnerModel ownerSpawner = this.plugin.getPlayerSpawnerCache().getCachedElements().stream().filter(ps -> ps.getSpawners().stream().anyMatch(s -> s.getLocation().equals(location))).findFirst().orElse(null);

        for (String world : plugin.getConfigs().getConfig().getStringList("worlds")) {
            if (block.getWorld().getName().equalsIgnoreCase(world)) {
                if (block.getType() == Material.MOB_SPAWNER) {
                    e.setCancelled(true);
                }
                return;
            }
        }

        if (ownerSpawner == null) {
            if (block.getType() == Material.MOB_SPAWNER) {
                ItemStack itemStack = player.getItemInHand();
                if (itemStack != null && itemStack.containsEnchantment(Enchantment.SILK_TOUCH)) {
                    player.getInventory().addItem(new ItemStack(Material.MOB_SPAWNER));
                    block.setType(Material.AIR);
                }
                return;
            }
            return;
        }

        if (!ownerSpawner.getPlayer().getUuid().equals(player.getUniqueId()) && !player.isOp()) {
            player.sendMessage(Messages.SPAWNER_NOT_OWNER.getMessage().replace("{owner}", ownerSpawner.getPlayer().getName()));
            e.setCancelled(true);
            return;
        }


        SpawnerModel spawner = ownerSpawner.getSpawners().stream().filter(s -> s.getLocation().equals(location)).findFirst().orElse(null);

        if (spawner == null) return;
        ownerSpawner.getSpawners().remove(spawner);
        this.plugin.getPlayerSpawnerCache().removeSpawnerFromChunk(spawner);
        this.plugin.getPlayerSpawnerStorage().delete(ownerSpawner, spawner);

        ItemStack tool = player.getItemInHand();
        if (tool != null && tool.containsEnchantment(Enchantment.SILK_TOUCH)) {
            player.getInventory().addItem(new ItemStack(Material.MOB_SPAWNER));
            player.getInventory().addItem(new ItemStack(Material.MONSTER_EGG, 1, spawner.getType().getTypeId()));
            player.sendMessage(Messages.SPAWNER_BREAK_SUCCESS.getMessage());
            block.setType(Material.AIR);
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        for (Entity entity : e.getChunk().getEntities()) {
            if (!(entity instanceof LivingEntity)) continue;
            LivingEntity living = (LivingEntity) entity;

            // Se já tem metadata, não precisa restaurar
            if (living.hasMetadata("stack-spawner")) continue;

            String name = living.getCustomName();
            if (name == null || name.isEmpty()) continue;

            // Verifica se o nome segue o padrão do plugin (ex: &e10x &7Zumbi)
            int count = getStackCount(living);
            if (count > 0 && name.contains("x")) {
                living.setMetadata("stack-spawner", new FixedMetadataValue(this.plugin, count));
                LocationUtils.freeze(living, this.plugin);
            }
        }
    }

    @EventHandler
    public void onSign(SignChangeEvent e) {
        Block below = e.getBlock().getLocation().clone().add(0, -1, 0).getBlock();
        if (below.getType() != Material.CHEST) return;

        Block spawnerBlock = below.getLocation().clone().add(0, -1, 0).getBlock();
        if (spawnerBlock.getType() != Material.MOB_SPAWNER) return;

        String written = e.getLine(0);
        if (written == null || written.isEmpty()) return;

        EntityType type;
        try {
            type = EntityType.valueOf(written.toUpperCase());
        } catch (IllegalArgumentException ex) {
            e.getPlayer().sendMessage("§cTipo de mob inválido!");
            e.getBlock().breakNaturally();
            return;
        }

        e.setLine(0, "§6[MobSpawn]");
        e.setLine(1, "");
        e.setLine(2, type.name().toUpperCase());
        e.setLine(3, "");
    }

    @EventHandler
    public void onMobDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof LivingEntity)) {
            return;
        }

        LivingEntity entity = (LivingEntity) e.getEntity();
        if (!entity.hasMetadata("stack-spawner")) {
            return;
        }

        if (e.getCause() == EntityDamageEvent.DamageCause.CUSTOM || e.getCause() == EntityDamageEvent.DamageCause.MAGIC) {
            e.setCancelled(true);
            return;
        }

        // Cooldown por jogador + entidade
        Entity damager = e.getDamager();
        if (damager instanceof Player) {
            String cooldownKey = damager.getUniqueId() + ":" + entity.getEntityId();
            long now = System.currentTimeMillis();
            Long lastHit = HIT_COOLDOWN.get(cooldownKey);
            if (lastHit != null && now - lastHit < HIT_COOLDOWN_MS) {
                e.setCancelled(true);
                return;
            }
            HIT_COOLDOWN.put(cooldownKey, now);
        }

        Arrow arrowToRemove = null;

        if (damager instanceof Arrow) {
            Arrow arrow = (Arrow) damager;
            if (!(arrow.getShooter() instanceof Player)) return;
            arrowToRemove = arrow;
            damager = arrow.getShooter();
        }

        if (damager instanceof Player) {
            McMMOPlayer mcPlayer = UserManager.getPlayer((Player) damager);
            if (mcPlayer == null) return;
        }


        // Passa a Arrow original — mcMMO precisa dela para identificar Archery
        if (arrowToRemove != null) {
            // Para flechas: só passa para o mcMMO processar se tiver as metadatas necessárias
            boolean hasRequiredMeta = arrowToRemove.hasMetadata("mcMMO: Bow Force")
                    && !arrowToRemove.getMetadata("mcMMO: Bow Force").isEmpty()
                    && arrowToRemove.hasMetadata("mcMMO: Arrow Distance")
                    && !arrowToRemove.getMetadata("mcMMO: Arrow Distance").isEmpty();

            assert damager instanceof Player;
            Player player = (Player) damager;
            if (hasRequiredMeta) {
                McMMOPlayer mcPlayer = UserManager.getPlayer(player);
                ArcheryManager archeryManager = mcPlayer.getArcheryManager();

                // Skill Shot
                if (archeryManager.canSkillShot()) {
                    e.setDamage(archeryManager.skillShotCheck(e.getDamage()));
                }

                // Track Arrows
                if (!arrowToRemove.hasMetadata("mcMMO: Infinite Arrow") && archeryManager.canTrackArrows()) {
                    archeryManager.trackArrows(entity);
                }

                // Calcula bônus de distância manualmente
                double distanceBonus = 0.0;
                try {
                    String distanceMeta = arrowToRemove.getMetadata("mcMMO: Arrow Distance").get(0).asString();
                    Location firedFrom = com.gmail.nossr50.skills.archery.Archery.stringToLocation(distanceMeta);
                    Location target = entity.getLocation();
                    if (firedFrom.getWorld() == target.getWorld()) {
                        distanceBonus = (int)(firedFrom.distanceSquared(target) * 0.025);
                    }
                } catch (Exception ex) {
                    // fallback sem bônus de distância
                }

                // XP base do mob * Bow Force + bônus de distância
                double bowForce = arrowToRemove.getMetadata("mcMMO: Bow Force").get(0).asDouble();
                double baseXp = com.gmail.nossr50.config.Config.getInstance().getCombatXP(entity.getType());
                if (baseXp <= 0) baseXp = 1.0;
                baseXp *= 10.0;
                baseXp *= bowForce;
                baseXp += distanceBonus;

                mcPlayer.getProfile().addExperience(SkillType.ARCHERY, (int) baseXp);
                // Verifica e processa level up
                com.gmail.nossr50.util.skills.SkillUtils.xpCheckSkill(SkillType.ARCHERY, player, mcPlayer.getProfile());

            } else {
                ExperienceAPI.addXP(player, SkillType.ARCHERY.name(), 10);
            }
        } else {
            // Melee: passa o player normalmente
            CombatUtils.processCombatAttack(e, damager, entity);
        }

        e.setCancelled(true);
        entity.setNoDamageTicks(0);

        if (damager instanceof Player) {
            Player player = (Player) damager;
            SpawnerModel spawner = getSpawnerByEntityMetadata(entity);
            if (spawner != null && spawner.getMcmmoXp() > 0) {
                if (arrowToRemove != null) {
                    // XP extra de Archery pelo spawner
                    ExperienceAPI.addXP(player, SkillType.ARCHERY.name(), (int) spawner.getMcmmoXp());
                } else {
                    ItemStack item = player.getItemInHand();
                    if (ToolType.SWORD.inHand(item)) {
                        ExperienceAPI.addXP(player, SkillType.SWORDS.name(), (int) spawner.getMcmmoXp());
                    } else if (ToolType.AXE.inHand(item)) {
                        ExperienceAPI.addXP(player, SkillType.AXES.name(), (int) spawner.getMcmmoXp());
                    }
                }
            }

            applyToolDurability(player);
        }

        if (arrowToRemove != null) {
            arrowToRemove.remove();
        }

        double damage = Math.max(0.0, e.getDamage());
        if (damage == 0 || !entity.isValid() || entity.isDead()) {
            return;
        }

        double newHealth = entity.getHealth() - damage;
        if (newHealth > 0) {
            entity.setHealth((int) Math.min(newHealth, 20.0));
            return;
        }

        decrementStackedMob(entity, damager);
    }

    private void decrementStackedMob(LivingEntity entity, Entity damager) {
        int stackCount = getStackCount(entity);
        Player killer = damager instanceof Player ? (Player) damager : null;
        SpawnerModel spawner = getSpawnerByEntityMetadata(entity);
        boolean killAll = this.plugin.getConfigCache().getPlugin().isStackMobsKillAll() && killer != null && !killer.isSneaking();
        int deaths = killAll ? stackCount : 1;

        List<ItemStack> drops = createCustomDrops(spawner, killer, deaths);
        giveOrDropItems(killer, entity.getLocation(), drops);

        if (killAll || stackCount <= 1) {
            entity.setMetadata(MANUAL_STACK_DEATH, new FixedMetadataValue(this.plugin, Boolean.TRUE));
            entity.setHealth(0);
            return;
        }

        int newStackCount = stackCount - 1;
        entity.setHealth(entity.getMaxHealth());
        entity.setMetadata("stack-spawner", new FixedMetadataValue(this.plugin, newStackCount));
        LocationUtils.updateCustomName(entity, this.plugin.getConfigCache().getPlugin().formatStackDisplayName(newStackCount, entity.getType()));
    }

    private SpawnerModel getSpawnerByEntityMetadata(LivingEntity entity) {
        if (!entity.hasMetadata("spawner-id")) {
            return null;
        }
        String spawnerId = entity.getMetadata("spawner-id").get(0).asString();
        return this.plugin.getSpawnerCache().getByID(spawnerId);
    }

    private List<ItemStack> createCustomDrops(SpawnerModel spawner, Player killer, int kills) {
        List<ItemStack> drops = new ArrayList<>();
        if (spawner == null || spawner.getDrops() == null || spawner.getDrops().isEmpty()) {
            return drops;
        }

        int lootingLevel = 0;
        if (killer != null && killer.getItemInHand() != null) {
            lootingLevel = killer.getItemInHand().getEnchantmentLevel(Enchantment.LOOT_BONUS_MOBS);
        }

        for (int i = 0; i < kills; i++) {
            for (DropModel dropModel : spawner.getDrops()) {
                if (Math.random() > dropModel.getChance()) {
                    continue;
                }

                int amount = dropModel.getMinAmount();
                if (dropModel.getMaxAmount() > dropModel.getMinAmount()) {
                    amount += (int) (Math.random() * (dropModel.getMaxAmount() - dropModel.getMinAmount() + 1));
                }

                if (lootingLevel > 0 && amount > 0) {
                    amount += (int) (Math.random() * (lootingLevel + 1));
                }

                if (amount > 0) {
                    ItemStack item = dropModel.getItem().clone();
                    item.setAmount(amount);
                    addDropSafely(drops, item);
                }
            }
        }
        return drops;
    }

    private void giveOrDropItems(Player killer, Location location, List<ItemStack> drops) {
        if (drops == null || drops.isEmpty()) {
            return;
        }

        List<ItemStack> validDrops = new ArrayList<>();
        for (ItemStack drop : drops) {
            if (drop != null && drop.getType() != Material.AIR && drop.getAmount() > 0) {
                validDrops.add(drop);
            }
        }

        if (validDrops.isEmpty()) {
            return;
        }

        if (killer == null) {
            for (ItemStack drop : validDrops) {
                location.getWorld().dropItemNaturally(location, drop);
            }
            return;
        }

        for (ItemStack leftover : killer.getInventory().addItem(validDrops.toArray(new ItemStack[0])).values()) {
            if (leftover != null && leftover.getType() != Material.AIR && leftover.getAmount() > 0) {
                killer.getWorld().dropItemNaturally(location, leftover);
            }
        }
    }

    private void applyToolDurability(Player player) {
        ItemStack item = player.getItemInHand();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        int unbreakingLevel = item.getEnchantmentLevel(Enchantment.DURABILITY);

        if (unbreakingLevel > 0) {
            double chanceDeNaoGastar = (double) unbreakingLevel / (unbreakingLevel + 1);
            if (Math.random() < chanceDeNaoGastar) {
                return;
            }
        }

        int maxDurability = item.getType().getMaxDurability();
        if (maxDurability <= 0) {
            return;
        }

        short nextDurability = (short) (item.getDurability() + 1);
        if (nextDurability >= maxDurability) {
            player.setItemInHand(new ItemStack(Material.AIR));
            return;
        }

        item.setDurability(nextDurability);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(EntityDeathEvent e) {
        if (e.getEntity() instanceof Player) return;
        LivingEntity entity = e.getEntity();
        if (entity.hasMetadata(MANUAL_STACK_DEATH)) {
            e.getDrops().clear();
            e.setDroppedExp(0);
            entity.removeMetadata(MANUAL_STACK_DEATH, this.plugin);
            entity.removeMetadata("stack-spawner", this.plugin);
            entity.removeMetadata("spawner-id", this.plugin);
            return;
        }

        int cont = getStackCount(entity);

        String spawnerId = entity.hasMetadata("spawner-id") ? entity.getMetadata("spawner-id").get(0).asString() : null;

        SpawnerModel spawner = (spawnerId != null) ? this.plugin.getSpawnerCache().getByID(spawnerId) : null;

        List<ItemStack> finalDrops = new ArrayList<>();
        List<ItemStack> eventDrops = e.getDrops();

        if (this.plugin.getConfigCache().getPlugin().isStackMobsKillAll() && entity.getKiller() != null && !entity.getKiller().isSneaking()) {
            Player killer = entity.getKiller();
            e.setDroppedExp(e.getDroppedExp() * cont);

            int lootingLevel = 0;
            if (killer.getItemInHand() != null) {
                lootingLevel = killer.getItemInHand().getEnchantmentLevel(Enchantment.LOOT_BONUS_MOBS);
            }

            if (spawner != null && spawner.getDrops() != null && !spawner.getDrops().isEmpty()) {
                for (int i = 0; i < cont; i++) {
                    for (DropModel dropModel : spawner.getDrops()) {
                        if (Math.random() <= dropModel.getChance()) {
                            int amount = dropModel.getMinAmount();
                            if (dropModel.getMaxAmount() > dropModel.getMinAmount()) {
                                amount += (int) (Math.random() * (dropModel.getMaxAmount() - dropModel.getMinAmount() + 1));
                            }

                            if (lootingLevel > 0 && amount > 0) {
                                amount += (int) (Math.random() * (lootingLevel + 1));
                            }

                            if (amount > 0) {
                                ItemStack item = dropModel.getItem().clone();
                                item.setAmount(amount);
                                addDropSafely(finalDrops, item);
                            }
                        }
                    }
                }
                eventDrops.clear();
                eventDrops.addAll(finalDrops);
            } else {
                List<ItemStack> vanillaDrops = new ArrayList<>(eventDrops);
                eventDrops.clear();
                for (ItemStack drop : vanillaDrops) {
                    if (drop == null || drop.getType() == Material.AIR) continue;
                    int totalAmount = drop.getAmount() * cont;
                    addDropSafely(finalDrops, drop, totalAmount);
                }
                eventDrops.addAll(finalDrops);
            }
        } else {
            int lootingLevel = 0;
            if (entity.getKiller() != null && entity.getKiller().getItemInHand() != null) {
                lootingLevel = entity.getKiller().getItemInHand().getEnchantmentLevel(Enchantment.LOOT_BONUS_MOBS);
            }

            if (spawner != null && spawner.getDrops() != null && !spawner.getDrops().isEmpty()) {
                eventDrops.clear();
                for (DropModel dropModel : spawner.getDrops()) {
                    if (Math.random() <= dropModel.getChance()) {
                        int amount = dropModel.getMinAmount();
                        if (dropModel.getMaxAmount() > dropModel.getMinAmount()) {
                            amount += (int) (Math.random() * (dropModel.getMaxAmount() - dropModel.getMinAmount() + 1));
                        }

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

                Location exactSpawnLocation = entity.getLocation();

                final LivingEntity spawned = (LivingEntity) exactSpawnLocation.getWorld().spawnEntity(exactSpawnLocation, e.getEntityType());

                spawned.setNoDamageTicks(0);

                spawned.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 2147483647, 127), true);

                spawned.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 2147483647, 127), true);

                spawned.setCustomName(this.plugin.getConfigCache().getPlugin().formatStackDisplayName(newCont, entity.getType()));
                spawned.setCustomNameVisible(true);

                LocationUtils.freeze(spawned, this.plugin);

                spawned.setMetadata("stack-spawner", new FixedMetadataValue(this.plugin, newCont));
                if (spawnerId != null) {
                    spawned.setMetadata("spawner-id", new FixedMetadataValue(this.plugin, spawnerId));
                }
            }
        }

        if (entity.getKiller() != null && !e.getDrops().isEmpty()) {
            Player killer = entity.getKiller();
            List<ItemStack> dropsToGive = new ArrayList<>(e.getDrops());

            e.getDrops().clear();
            for (ItemStack leftover : killer.getInventory().addItem(dropsToGive.toArray(new ItemStack[0])).values()) {
                killer.getWorld().dropItemNaturally(entity.getLocation(), leftover);
            }
        }

        // No início do onDeath
        String entityId = String.valueOf(entity.getEntityId());
        HIT_COOLDOWN.entrySet().removeIf(entry -> entry.getKey().endsWith(":" + entityId));

        entity.removeMetadata("stack-spawner", this.plugin);
        entity.removeMetadata("spawner-id", this.plugin);
    }

    private int getStackCount(LivingEntity entity) {
        if (entity.hasMetadata("stack-spawner")) {
            return entity.getMetadata("stack-spawner").get(0).asInt();
        }

        if (!entity.hasMetadata("spawner-id") && entity.getCustomName() == null) {
            return 1;
        }

        String name = entity.getCustomName();
        if (name != null && !name.isEmpty()) {
            try {
                String stripped = ChatColor.stripColor(name);

                if (stripped.contains("x")) {
                    String[] parts = stripped.split("x");
                    if (parts.length > 0) {
                        String countPart = parts[0].trim();

                        countPart = countPart.replaceAll("[^0-9]", "");
                        if (!countPart.isEmpty()) {
                            return Integer.parseInt(countPart);
                        }
                    }
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }
        return 1;
    }

    private void addDropSafely(List<ItemStack> drops, ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) return;

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