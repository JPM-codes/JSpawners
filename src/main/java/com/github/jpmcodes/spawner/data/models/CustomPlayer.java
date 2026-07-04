package com.github.jpmcodes.spawner.data.models;

import java.util.UUID;
import lombok.Generated;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Getter
@Setter
public class CustomPlayer {
    private final UUID uuid;
    private final String name;
}