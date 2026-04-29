package com.github.jpmcodes.spawner.data.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class PlayerSpawnerModel {
    private final CustomPlayer player;
    private List<SpawnerModel> spawners;

    public boolean hasSpawners() {
        return spawners != null && !spawners.isEmpty();
    }

    public void add(SpawnerModel spawner) {
        spawners.add(spawner);
    }
}
