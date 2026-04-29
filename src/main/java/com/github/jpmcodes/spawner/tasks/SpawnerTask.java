package com.github.jpmcodes.spawner.tasks;

import com.github.jpmcodes.spawner.JSpawnerPlugin;
import com.github.jpmcodes.spawner.data.models.PlayerSpawnerModel;
import com.github.jpmcodes.spawner.data.models.SpawnerModel;
import com.github.jpmcodes.spawner.utils.Configs;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpawnerTask extends BukkitRunnable {

    private static final Map<String, Long> NEXT_SPAWN_TICK = new HashMap<String, Long>();
    private static final Map<UUID, Integer> STACKED_MOBS = new HashMap<UUID, Integer>();

    private final JSpawnerPlugin plugin;

    public SpawnerTask(JSpawnerPlugin plugin) {
        this.plugin = plugin;
    }

    public static int getStackSize(Entity entity) {
        Integer amount = STACKED_MOBS.get(entity.getUniqueId());
        return amount == null ? 1 : amount;
    }

    public static void clearStack(Entity entity) {
        STACKED_MOBS.remove(entity.getUniqueId());
    }

    @Override
    public void run() {
        long nowTick = plugin.getServer().getWorlds().isEmpty()
                ? 0L
                : plugin.getServer().getWorlds().get(0).getFullTime();

        int activationRange = plugin.getConfigs().getInt("activation-range");
        int stackRadius = plugin.getConfigs().getInt("stack-mobs.stack-radius");
        int maxStackSize = plugin.getConfigs().getInt("stack-mobs.max-stack-size");
        boolean stackEnabled = plugin.getConfigs().getBoolean("stack-mobs.enable");
        String displayName = Configs.toChatMessage(plugin.getConfigs().getConfig().getString("stack-mobs.display-name", "&e{count}x &7{mob}"));

        for (PlayerSpawnerModel playerSpawner : plugin.getPlayerSpawnerCache().getCachedElements()) {
            Player owner = plugin.getServer().getPlayer(playerSpawner.getPlayer().getUuid());
            if (owner == null || !owner.isOnline() || !playerSpawner.hasSpawners()) continue;

            for (SpawnerModel spawner : playerSpawner.getSpawners()) {
                Location location = spawner.getLocation();
                if (location == null || location.getWorld() == null) continue;
                if (!owner.getWorld().equals(location.getWorld()) || owner.getLocation().distanceSquared(location) > (activationRange * activationRange)) continue;

                String key = Configs.saveLocation(location);
                Long nextTick = NEXT_SPAWN_TICK.get(key);
                if (nextTick != null && nowTick < nextTick) continue;

                int amountToSpawn = Math.max(1, spawner.getSpawnCount());
                LivingEntity stackedTarget = null;

                if (stackEnabled) {
                    for (Entity near : location.getWorld().getNearbyEntities(location, stackRadius, stackRadius, stackRadius)) {
                        if (!(near instanceof LivingEntity) || near.isDead()) continue;
                        if (near.getType() == spawner.getType()) {
                            stackedTarget = (LivingEntity) near;
                            break;
                        }
                    }
                }

                if (stackEnabled && stackedTarget != null) {
                    int current = getStackSize(stackedTarget);
                    int finalAmount = Math.min(maxStackSize, current + amountToSpawn);
                    STACKED_MOBS.put(stackedTarget.getUniqueId(), finalAmount);
                    stackedTarget.setCustomName(displayName
                            .replace("{count}", String.valueOf(finalAmount))
                            .replace("{mob}", spawner.getType().name()));
                    stackedTarget.setCustomNameVisible(true);
                } else {
                    for (int i = 0; i < amountToSpawn; i++) {
                        LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, spawner.getType());
                        if (stackEnabled) {
                            STACKED_MOBS.put(entity.getUniqueId(), 1);
                        }
                    }
                }

                long delay = spawner.getMinSpawnDelay();
                if (spawner.getMaxSpawnDelay() > spawner.getMinSpawnDelay()) {
                    delay += (long) (Math.random() * (spawner.getMaxSpawnDelay() - spawner.getMinSpawnDelay()));
                }
                NEXT_SPAWN_TICK.put(key, nowTick + Math.max(1L, delay));
            }
        }

    }

}
