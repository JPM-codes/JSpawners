package com.github.jpmcodes.spawner.data.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class PlayerSpawnerModel {
    private final CustomPlayer player;
    private List<SpawnerModel> spawners;
    public boolean hasSpawners() {
        return (this.spawners != null && !this.spawners.isEmpty());
    }

    public void add(SpawnerModel spawner) {
        this.spawners.add(spawner);
    }
}