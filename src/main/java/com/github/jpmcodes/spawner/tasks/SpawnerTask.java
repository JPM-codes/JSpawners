package com.github.jpmcodes.spawner.tasks;

import com.github.jpmcodes.spawner.JSpawnerPlugin;
import com.github.jpmcodes.spawner.data.models.PlayerSpawnerModel;
import com.github.jpmcodes.spawner.data.models.SpawnerModel;
import com.github.jpmcodes.spawner.utils.Configs;
import com.github.jpmcodes.spawner.utils.LocationUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

public class SpawnerTask extends BukkitRunnable {

    private static final Map<String, Long> NEXT_SPAWN_TICK = new HashMap<>();

    private final JSpawnerPlugin plugin;

    public SpawnerTask(JSpawnerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        // 1. Congelar todos os mobs de spawner ativos para evitar empurrões ou movimentos residuais
        for (World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof LivingEntity && entity.hasMetadata("mob_spawner")) {
                    entity.setVelocity(new Vector(0, 0, 0));
                }
            }
        }

        long nowTick = plugin.getServer().getWorlds().isEmpty()
                ? 0L
                : plugin.getServer().getWorlds().get(0).getFullTime();

        int activationRange = plugin.getConfigs().getInt("activation-range");

        for (PlayerSpawnerModel playerSpawner : plugin.getPlayerSpawnerCache().getCachedElements()) {
            Player owner = plugin.getServer().getPlayer(playerSpawner.getPlayer().getName());
            if (owner == null || !owner.isOnline() || !playerSpawner.hasSpawners()) continue;

            for (SpawnerModel spawner : playerSpawner.getSpawners()) {
                Location location = spawner.getLocation();
                if (location == null || location.getWorld() == null) continue;
                if (!owner.getWorld().equals(location.getWorld()) || owner.getLocation().distanceSquared(location) > (activationRange * activationRange))
                    continue;

                Location spawnLocation = location;
                String spawnPath = "locais_nascimento." + playerSpawner.getPlayer().getUuid() + "." + spawner.getType().name();
                if (plugin.getSavesConfig().contains(spawnPath)) {
                    spawnLocation = plugin.getSavesConfig().getLocation(spawnPath);
                }

                String key = Configs.saveLocation(location);
                Long nextTick = NEXT_SPAWN_TICK.get(key);
                if (nextTick != null && nowTick < nextTick) continue;

                int amountToSpawn = Math.max(1, plugin.getConfigs().getInt("spawners.spawn-count"));

                LivingEntity stackedTarget = LocationUtils.getNearbyLivingEntity(spawnLocation, plugin.getConfigs().getInt("stack-mobs.stack-radius"), spawner.getType());

                boolean enableStack = plugin.getConfigs().getBoolean("stack-mobs.enable");

                if (enableStack) {
                    if (stackedTarget != null) {
                        int currentAmount = stackedTarget.hasMetadata("stack-spawner")
                                ? stackedTarget.getMetadata("stack-spawner").get(0).asInt()
                                : 1;

                        int newAmount = currentAmount + amountToSpawn;

                        if (newAmount > plugin.getConfigs().getInt("stack-mobs.max-stack-size")) {
                            newAmount = plugin.getConfigs().getInt("stack-mobs.max-stack-size");
                        }

                        stackedTarget.setCustomName(plugin.getConfigs().getString("stack-mobs.display-name")
                                .replace("&", "§")
                                .replace("{count}", String.valueOf(newAmount))
                                .replace("{mob}", spawner.getType().name()));
                        stackedTarget.setCustomNameVisible(true);


                        stackedTarget.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 100));
                        stackedTarget.setVelocity(new Vector(0, 0, 0));
                        LocationUtils.setNoAI(stackedTarget);
                        stackedTarget.setMetadata("mob_spawner", new FixedMetadataValue(plugin, true));
                        stackedTarget.setMetadata("stack-spawner", new FixedMetadataValue(plugin, newAmount));
                        stackedTarget.setMetadata("spawner-id", new FixedMetadataValue(plugin, spawner.getId()));

                    } else {
                        Entity entity = spawnLocation.getWorld().spawnEntity(spawnLocation, spawner.getType());

                        LivingEntity livingEntity = (LivingEntity) entity;
                        livingEntity.setCustomName(plugin.getConfigs().getString("stack-mobs.display-name")
                                .replace("&", "§")
                                .replace("{count}", String.valueOf(amountToSpawn))
                                .replace("{mob}", spawner.getType().name()));
                        livingEntity.setCustomNameVisible(true);

                        livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 100));
                        entity.setVelocity(new Vector(0, 0, 0));
                        LocationUtils.setNoAI(entity);
                        entity.setMetadata("stack-spawner", new FixedMetadataValue(plugin, amountToSpawn));
                        entity.setMetadata("mob_spawner", new FixedMetadataValue(plugin, true));
                        entity.setMetadata("spawner-id", new FixedMetadataValue(plugin, spawner.getId()));
                    }
                } else {
                    for (int i = 0; i < amountToSpawn; i++) {
                        Entity entity = spawnLocation.getWorld().spawnEntity(spawnLocation, spawner.getType());
                        entity.setVelocity(new Vector(0, 0, 0));
                        LocationUtils.setNoAI(entity);
                        entity.setMetadata("mob_spawner", new FixedMetadataValue(plugin, true));
                        entity.setMetadata("spawner-id", new FixedMetadataValue(plugin, spawner.getId()));
                    }
                }
                long delay = plugin.getConfigs().getInt("spawners.min-delay");
                if (plugin.getConfigs().getInt("spawners.max-delay") > plugin.getConfigs().getInt("spawners.min-delay")) {
                    delay += (long) (Math.random() * (plugin.getConfigs().getInt("spawners.max-delay") - plugin.getConfigs().getInt("spawners.min-delay")));
                }
                NEXT_SPAWN_TICK.put(key, nowTick + Math.max(1L, delay));
            }
        }
    }
}