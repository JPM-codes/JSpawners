package com.github.jpmcodes.spawner.data.cache;

import com.github.jpmcodes.spawner.data.models.PlayerSpawnerModel;
import com.github.jpmcodes.spawner.utils.Cache;

import java.util.UUID;

public class PlayerSpawnerCache extends Cache<PlayerSpawnerModel> {

    public PlayerSpawnerModel getByPlayerUUID(UUID uuid) {
        return getCached($ -> $.getPlayer().getUuid().equals(uuid));
    }

     public PlayerSpawnerModel getByPlayerName(String name) {
        return getCached($ -> $.getPlayer().getName().equalsIgnoreCase(name));
    }



}
