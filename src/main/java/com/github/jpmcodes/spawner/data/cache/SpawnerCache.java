package com.github.jpmcodes.spawner.data.cache;

import com.github.jpmcodes.spawner.data.models.SpawnerModel;
import com.github.jpmcodes.spawner.utils.Cache;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public class SpawnerCache extends Cache<SpawnerModel> {

    public SpawnerModel getByID(String id) {
        return getCached($ -> $.getId().equalsIgnoreCase(id));
    }
    public SpawnerModel getByItem(ItemStack item) {
        return getCached($ -> $.getItem().isSimilar(item));
    }

    public SpawnerModel getByLocation(Location location) {
        return getCached($ -> $.getLocation().equals(location));
    }
}
