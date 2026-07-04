package com.github.jpmcodes.spawner.data.cache;

import com.github.jpmcodes.spawner.data.models.CustomPlayer;
import com.github.jpmcodes.spawner.utils.Cache;
import java.util.UUID;

public class CustomPlayerCache
        extends Cache<CustomPlayer> {
    public CustomPlayer getByUUID(UUID uuid) {
        return (CustomPlayer) getCached($ -> $.getUuid().equals(uuid));
    }

    public CustomPlayer getByName(String name) {
        return (CustomPlayer) getCached($ -> $.getName().equalsIgnoreCase(name));
    }
}