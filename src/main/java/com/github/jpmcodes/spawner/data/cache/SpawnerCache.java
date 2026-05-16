package com.github.jpmcodes.spawner.data.cache;

import com.github.jpmcodes.spawner.data.models.SpawnerModel;
import com.github.jpmcodes.spawner.utils.Cache;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

public class SpawnerCache extends Cache<SpawnerModel> {

    public SpawnerModel getByID(String id) {
        return getCached($ -> $.getId().equalsIgnoreCase(id));
    }

    public SpawnerModel getByEgg(ItemStack item) {
        if (item == null || item.getType() != Material.MONSTER_EGG) {
            return null;
        }

        short data = item.getData().getData();

        EntityType type = EntityType.fromId(data);
        if (type == null) return null;


        return getCached($ -> $.getType() == type);
    }

    public SpawnerModel getByLocation(Location location) {
        return getCached($ -> $.getLocation().equals(location));
    }
}
