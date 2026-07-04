package com.github.jpmcodes.spawner.data.models;

import com.github.jpmcodes.spawner.data.models.drop.DropModel;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

@Getter
@Setter
@AllArgsConstructor
public class SpawnerModel implements Cloneable {
    private final String id;
    private EntityType type;
    private List<DropModel> drops;
    private double mcmmoXp;
    private Location location;
    private UUID ownerUuid;

    public SpawnerModel clone() {
        try {
            return (SpawnerModel) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SpawnerModel that = (SpawnerModel) o;
        return Objects.equals(this.id, that.id) && Objects.equals(this.location, that.location);
    }

    public int hashCode() {
        int result = (this.id != null) ? this.id.hashCode() : 0;
        result = 31 * result + (this.location != null ? this.location.hashCode() : 0);
        return result;
    }
}
