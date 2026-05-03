package com.github.jpmcodes.spawner.tasks;

import com.github.jpmcodes.spawner.JSpawnerPlugin;
import com.github.jpmcodes.spawner.data.models.PlayerSpawnerModel;
import com.github.jpmcodes.spawner.data.models.SpawnerModel;
import com.github.jpmcodes.spawner.utils.Configs;
import net.minecraft.server.v1_5_R3.NBTTagCompound;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_5_R3.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

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

                int amountToSpawn = Math.max(1, spawner.getSpawnCount());

                LivingEntity stackedTarget = getNearbyLivingEntity(spawnLocation, plugin.getConfigs().getInt("stack-mobs.stack-radius"), spawner.getType());

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


                        if (plugin.getConfig().getBoolean("mobs.disable-movement")) {
                            stackedTarget.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 100));
                        }
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

                        if (plugin.getConfig().getBoolean("mobs.disable-movement")) {
                            livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 100));
                        }
                        entity.setMetadata("stack-spawner", new FixedMetadataValue(plugin, amountToSpawn));
                        entity.setMetadata("mob_spawner", new FixedMetadataValue(plugin, true));
                        entity.setMetadata("spawner-id", new FixedMetadataValue(plugin, spawner.getId()));
                    }
                } else {
                    for (int i = 0; i < amountToSpawn; i++) {
                        spawnLocation.getWorld().spawnEntity(spawnLocation, spawner.getType());
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

    /**
     * Busca uma entidade viva específica próxima a uma localização.
     * * @param location A localização central da busca.
     *
     * @param radius O raio de distância máximo em blocos.
     * @param type   O tipo da entidade que estamos procurando.
     * @return A LivingEntity encontrada, ou null se não encontrar nenhuma.
     */
    public LivingEntity getNearbyLivingEntity(Location location, double radius, EntityType type) {
        World world = location.getWorld();
        double radiusSquared = radius * radius;

        int centerChunkX = location.getBlockX() >> 4;
        int centerChunkZ = location.getBlockZ() >> 4;
        int chunkRadius = (int) Math.ceil(radius / 16.0);

        for (int x = centerChunkX - chunkRadius; x <= centerChunkX + chunkRadius; x++) {
            for (int z = centerChunkZ - chunkRadius; z <= centerChunkZ + chunkRadius; z++) {

                if (!world.isChunkLoaded(x, z)) continue;

                for (Entity near : world.getChunkAt(x, z).getEntities()) {
                    if (near.getLocation().distanceSquared(location) > radiusSquared) continue;
                    if (!(near instanceof LivingEntity) || near.isDead()) continue;

                    // Se encontrou o tipo que estamos procurando, retorna ele na hora!
                    if (near.getType() == type) {
                        return (LivingEntity) near;
                    }
                }
            }
        }
        return null;
    }


    public void setNoAI(Entity bukkitEntity) {
        // 1. Converte a entidade do Bukkit para a entidade interna do Minecraft
        net.minecraft.server.v1_5_R3.Entity nmsEntity = ((CraftEntity) bukkitEntity).getHandle();

        // 2. Cria a tag NBT para salvar as informações
        NBTTagCompound tag = new NBTTagCompound();

        // 3. Copia as informações atuais do mob para a tag
        nmsEntity.c(tag);

        // 4. Adiciona a configuração NoAI = 1 (Verdadeiro)
        tag.setInt("NoAI", 1);

        // 5. Aplica a tag de volta no mob
        nmsEntity.f(tag);
    }
}