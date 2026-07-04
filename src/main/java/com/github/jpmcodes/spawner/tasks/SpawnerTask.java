package com.github.jpmcodes.spawner.tasks;

import com.github.jpmcodes.egggolem.data.EggGolem;
import com.github.jpmcodes.spawner.JSpawnerPlugin;
import com.github.jpmcodes.spawner.config.PluginConfigSnapshot;
import com.github.jpmcodes.spawner.data.models.PlayerSpawnerModel;
import com.github.jpmcodes.spawner.data.models.SpawnerModel;
import com.github.jpmcodes.spawner.utils.Configs;
import com.github.jpmcodes.spawner.utils.LocationUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class SpawnerTask extends BukkitRunnable {
    private static final Map<String, Long> NEXT_SPAWN_TICK = new HashMap<>();
    private final JSpawnerPlugin plugin;
    public static boolean isSpawning = false;

    public SpawnerTask(JSpawnerPlugin plugin) {
        this.plugin = plugin;
    }

    public void run() {
        PluginConfigSnapshot cfg = this.plugin.getConfigCache().getPlugin();
        long nowTick = this.plugin.getServer().getWorlds().isEmpty() ? 0L
                : this.plugin.getServer().getWorlds().get(0).getFullTime();

        int activationRange = cfg.getActivationRange();
        int rangeSquared = activationRange * activationRange;

        Map<String, List<Location>> playersByWorld = new HashMap<>();
        for (Player player : this.plugin.getServer().getOnlinePlayers()) {
            String worldName = player.getWorld().getName();
            playersByWorld.computeIfAbsent(worldName, k -> new ArrayList<>()).add(player.getLocation());
        }

        for (PlayerSpawnerModel playerSpawner : this.plugin.getPlayerSpawnerCache().getCachedElements()) {
            Map<String, SpawnerGroup> groups = new HashMap<>();

            for (SpawnerModel spawner : playerSpawner.getSpawners()) {
                Location location = spawner.getLocation();
                if (location == null || location.getWorld() == null) continue;
                if (location.getBlock().getType() != org.bukkit.Material.MOB_SPAWNER) continue;

                boolean hasNearbyPlayer = false;
                List<Location> onlinePlayersInWorld = playersByWorld.get(location.getWorld().getName());
                if (onlinePlayersInWorld != null) {
                    for (Location pLoc : onlinePlayersInWorld) {
                        if (pLoc.distanceSquared(location) <= rangeSquared) {
                            hasNearbyPlayer = true;
                            break;
                        }
                    }
                }
                if (!hasNearbyPlayer) continue;

                String spawnerKey = Configs.saveLocation(location);
                Long nextTick = NEXT_SPAWN_TICK.get(spawnerKey);
                if (nextTick != null && nowTick < nextTick) continue;

                Location spawnLocation = location;
                String spawnPath = "locais_nascimento." + playerSpawner.getPlayer().getUuid() + "." + spawner.getType().name();
                Location cachedSpawn = this.plugin.getSpawnLocationCache().get(spawnPath);
                if (cachedSpawn != null && cachedSpawn.getWorld() != null) {
                    spawnLocation = cachedSpawn;
                }

                if (spawnLocation.getWorld() == null) continue;

                String groupKey = spawnLocation.getWorld().getName() + ":" + spawnLocation.getBlockX() + ":"
                        + spawnLocation.getBlockY() + ":" + spawnLocation.getBlockZ() + ":" + spawner.getType().name();

                SpawnerGroup group = groups.get(groupKey);
                if (group == null) {
                    group = new SpawnerGroup(spawnLocation, location, spawner.getType(), spawner.getId());
                    groups.put(groupKey, group);
                }
                group.spawnerKeys.add(spawnerKey);
            }

            for (SpawnerGroup group : groups.values()) {
                Location spawnLocation = group.location;
                int amountToSpawn = getAmountFromChest(group.spawnerLocation, group.type);
                if (amountToSpawn <= 0) continue;

                double searchRadius = cfg.getStackMobsRadius();
                List<LivingEntity> nearby = LocationUtils.getNearbyLivingEntities(spawnLocation, searchRadius, group.type);

                if (!nearby.isEmpty()) {
                    LivingEntity stackedTarget = nearby.get(0);

                    // Garante que o target ainda é válido
                    if (!stackedTarget.isValid()) continue;

                    int currentAmount = stackedTarget.hasMetadata("stack-spawner")
                            ? stackedTarget.getMetadata("stack-spawner").get(0).asInt()
                            : 1;

                    for (int i = 1; i < nearby.size(); i++) {
                        LivingEntity other = nearby.get(i);
                        if (other == null || !other.isValid()) continue;
                        currentAmount += other.hasMetadata("stack-spawner")
                                ? other.getMetadata("stack-spawner").get(0).asInt()
                                : 1;
                        other.remove();
                    }

                    int newAmount = Math.min(currentAmount + amountToSpawn, cfg.getStackMobsMaxStackSize());

                    LocationUtils.updateCustomName(stackedTarget, cfg.formatStackDisplayName(newAmount, group.type));
                    LocationUtils.freeze(stackedTarget, this.plugin);

                    stackedTarget.setMetadata("stack-spawner", new FixedMetadataValue(this.plugin, newAmount));
                    stackedTarget.setMetadata("spawner-id", new FixedMetadataValue(this.plugin, group.firstSpawnerId));

                } else {
                    // Na hora de spawnar no SpawnerTask:
                    SpawnerTask.isSpawning = true;
                    Entity entity = spawnLocation.getWorld().spawnEntity(spawnLocation, group.type);
                    SpawnerTask.isSpawning = false;

                    // Garante que é uma LivingEntity antes do cast
                    if (!(entity instanceof LivingEntity)) {
                        entity.remove();
                        continue;
                    }

                    LivingEntity livingEntity = (LivingEntity) entity;
                    livingEntity.removeMetadata("mcMMO: Spawned Entity", this.plugin);

                    LocationUtils.updateCustomName(livingEntity, cfg.formatStackDisplayName(amountToSpawn, group.type));
                    LocationUtils.freeze(livingEntity, this.plugin);

                    livingEntity.setMetadata("stack-spawner", new FixedMetadataValue(this.plugin, amountToSpawn));
                    livingEntity.setMetadata("spawner-id", new FixedMetadataValue(this.plugin, group.firstSpawnerId));
                }

                long delay = cfg.nextSpawnDelay(ThreadLocalRandom.current());
                for (String key : group.spawnerKeys) {
                    NEXT_SPAWN_TICK.put(key, nowTick + Math.max(1L, delay));
                }
            }
        }
    }

    private static class SpawnerGroup {
        final Location location;       // local de nascimento
        final Location spawnerLocation; // localização real do spawner
        final org.bukkit.entity.EntityType type;
        final String firstSpawnerId;
        final List<String> spawnerKeys = new ArrayList<>();

        SpawnerGroup(Location location, Location spawnerLocation, org.bukkit.entity.EntityType type, String firstSpawnerId) {
            this.location = location;
            this.spawnerLocation = spawnerLocation;
            this.type = type;
            this.firstSpawnerId = firstSpawnerId;
        }
    }

    private int getAmountFromChest(Location spawnerLocation, EntityType type) {
        Location chestLoc = spawnerLocation.clone().add(0, 1, 0);
        Block chestBlock = chestLoc.getBlock();
        if (chestBlock.getType() != Material.CHEST) {
            return 0;
        }

        Location signLoc = spawnerLocation.clone().add(0, 2, 0);
        Block signBlock = signLoc.getBlock();
        if (signBlock.getType() != Material.SIGN_POST) {
            return 0;
        }

        Sign sign = (Sign) signBlock.getState();
        String signType = ChatColor.stripColor(sign.getLine(2)).trim().toUpperCase();
        if (!signType.equals(type.name())) {
            return 0;
        }

        Chest chest = (Chest) chestBlock.getState();
        int eggCount = 0;
        short expectedData = type.getTypeId();

        // Carrega o ovo customizado de Golem na memória antes de checar o baú
        EggGolem ovoDeGolem = null;
        if (type == EntityType.IRON_GOLEM) {
            ovoDeGolem = EggGolem.load();
        }

        for (ItemStack item : chest.getInventory().getContents()) {
            if (item == null) continue;

            // Se o spawner for de Iron Golem, usamos a verificação da sua classe EggGolem
            if (type == EntityType.IRON_GOLEM && ovoDeGolem != null) {
                if (ovoDeGolem.hasEgg(item)) {
                    eggCount += item.getAmount();
                }
                continue; // Pula para o próximo item do baú
            }

            // Se for um spawner de outro mob (zumbi, esqueleto, etc), usa a lógica Vanilla
            if (item.getType() != Material.MONSTER_EGG) continue;
            if (item.getData().getData() == (byte) expectedData) {
                eggCount += item.getAmount();
            }
        }

        return eggCount;
    }

}
