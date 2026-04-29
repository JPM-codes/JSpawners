package com.github.jpmcodes.spawner.data.models;

import com.github.jpmcodes.spawner.data.models.drop.DropModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class SpawnerModel implements Cloneable {
    private final String id;
    private EntityType type;
    private ItemStack item;
    private int minSpawnDelay;
    private int maxSpawnDelay;
    private int spawnCount;
    private int spawnRange;
    private List<DropModel> drops;
    private Location location;

    @Override
    public SpawnerModel clone() {
        try {
            return (SpawnerModel) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
