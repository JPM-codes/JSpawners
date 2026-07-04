package com.github.jpmcodes.spawner.data.cache;

import com.github.jpmcodes.egggolem.JEggGolemPlugin;
import com.github.jpmcodes.spawner.data.models.SpawnerModel;
import com.github.jpmcodes.spawner.utils.Cache;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

public class SpawnerCache
        extends Cache<SpawnerModel> {
    public SpawnerModel getByID(String id) {
        return (SpawnerModel) getCached($ -> $.getId().equalsIgnoreCase(id));
    }

    public SpawnerModel getByEgg(ItemStack item) {
        if (item == null) {
            return null;
        }

        EntityType type;
        // Verifica se o item é o ovo do golem (la ele)
        if (JEggGolemPlugin.getInstance().getEggGolem().hasEgg(item)) {
            type = EntityType.IRON_GOLEM;
        } else if (item.getType() == Material.MONSTER_EGG) {
            short data = item.getData().getData();
            type = EntityType.fromId(data);
        } else {
            type = null;
        }

        if (type == null) {
            return null;
        }

        com.github.jpmcodes.spawner.utils.Debug.info("[Debug] Spawner lookup for type: " + type);
        SpawnerModel model = getCached($ -> ($.getType() == type));
        com.github.jpmcodes.spawner.utils.Debug.info("[Debug] Spawner model found: " + (model != null));
        return model;
    }

    public SpawnerModel getByLocation(Location location) {
        return getCached($ -> $.getLocation().equals(location));
    }
}
