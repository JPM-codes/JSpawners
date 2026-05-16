package com.github.jpmcodes.spawner.data.models;

import com.github.jpmcodes.spawner.data.models.drop.DropModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class SpawnerModel implements Cloneable {
    private final String id;
    private EntityType type;
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
