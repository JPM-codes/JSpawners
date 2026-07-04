package com.github.jpmcodes.spawner.data.cache;

import com.github.jpmcodes.spawner.data.models.PlayerSpawnerModel;
import com.github.jpmcodes.spawner.data.models.SpawnerModel;
import com.github.jpmcodes.spawner.utils.Cache;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerSpawnerCache extends Cache<PlayerSpawnerModel> {
    private final Map<String, List<SpawnerModel>> spawnersPerChunk = new HashMap<>();

    public PlayerSpawnerModel getByPlayerUUID(UUID uuid) {
        return (PlayerSpawnerModel) getCached($ -> $.getPlayer().getUuid().equals(uuid));
    }

    public PlayerSpawnerModel getByPlayerName(String name) {
        return (PlayerSpawnerModel) getCached($ -> $.getPlayer().getName().equalsIgnoreCase(name));
    }

    public String getChunkKey(SpawnerModel spawner) {
        if (spawner.getLocation() == null || spawner.getLocation().getWorld() == null)
            return "";

        int chunkX = spawner.getLocation().getBlockX() >> 4;
        int chunkZ = spawner.getLocation().getBlockZ() >> 4;
        return spawner.getLocation().getWorld().getName() + ":" + chunkX + ":" + chunkZ;
    }

    public void addSpawnerToChunk(SpawnerModel spawner) {
        String chunkKey = getChunkKey(spawner);
        ((List<SpawnerModel>) this.spawnersPerChunk.computeIfAbsent(chunkKey, k -> new ArrayList())).add(spawner);
    }

    public void removeSpawnerFromChunk(SpawnerModel spawner) {
        String chunkKey = getChunkKey(spawner);
        List<SpawnerModel> spawnersInChunk = this.spawnersPerChunk.get(chunkKey);

        if (spawnersInChunk != null) {
            spawnersInChunk.removeIf(s -> s.getLocation().equals(spawner.getLocation()));

            if (spawnersInChunk.isEmpty()) {
                this.spawnersPerChunk.remove(chunkKey);
            }
        }
    }

    public List<SpawnerModel> getSpawnersInChunk(String chunkKey) {
        return this.spawnersPerChunk.get(chunkKey);
    }
}