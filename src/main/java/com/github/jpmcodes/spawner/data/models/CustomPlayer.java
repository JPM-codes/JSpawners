package com.github.jpmcodes.spawner.data.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public class CustomPlayer {
    private final UUID uuid;
    private final String name;
}
