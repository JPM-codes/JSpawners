package com.github.jpmcodes.spawner.config;

import com.github.jpmcodes.spawner.utils.Configs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.bukkit.entity.EntityType;

public final class PluginConfigSnapshot {
    private final int engineTickInterval;
    private final int activationRange;
    private final int spawnersMinDelay;
    private final int spawnersMaxDelay;
    private final int spawnersSpawnCount;
    private final boolean stackMobsEnable;
    private final int stackMobsRadius;
    private final int stackMobsMaxStackSize;
    private final String stackMobsDisplayNameTemplate;
    private final boolean stackMobsKillAll;
    private final boolean debug;
    private final boolean bloquearMobsNascerem;
    private final List<String> blockedWorlds;
    private final List<String> blockedRegions;

    PluginConfigSnapshot(
            int engineTickInterval,
            int activationRange,
            int spawnersMinDelay,
            int spawnersMaxDelay,
            int spawnersSpawnCount,
            boolean stackMobsEnable,
            int stackMobsRadius,
            int stackMobsMaxStackSize,
            String stackMobsDisplayNameTemplate,
            boolean stackMobsKillAll,
            boolean debug,
            boolean bloquearMobsNascerem,
            List<String> blockedWorlds,
            List<String> blockedRegions) {
        this.engineTickInterval = Math.max(1, engineTickInterval);
        this.activationRange = Math.max(1, activationRange);
        this.spawnersMinDelay = Math.max(1, spawnersMinDelay);
        this.spawnersMaxDelay = Math.max(this.spawnersMinDelay, spawnersMaxDelay);
        this.spawnersSpawnCount = Math.max(1, spawnersSpawnCount);
        this.stackMobsEnable = stackMobsEnable;
        this.stackMobsRadius = Math.max(1, stackMobsRadius);
        this.stackMobsMaxStackSize = Math.max(1, stackMobsMaxStackSize);
        this.stackMobsDisplayNameTemplate = translate(stackMobsDisplayNameTemplate);
        this.stackMobsKillAll = stackMobsKillAll;
        this.debug = debug;
        this.bloquearMobsNascerem = bloquearMobsNascerem;
        this.blockedWorlds = Collections.unmodifiableList(filterNonEmpty(blockedWorlds));
        this.blockedRegions = Collections.unmodifiableList(filterNonEmpty(blockedRegions));
    }

    public static PluginConfigSnapshot from(Configs config) {
        return new PluginConfigSnapshot(
                config.getInt("engine-tick-interval"),
                config.getInt("activation-range"),
                config.getInt("spawners.min-delay"),
                config.getInt("spawners.max-delay"),
                config.getInt("spawners.spawn-count"),
                config.getBoolean("stack-mobs.enable"),
                config.getInt("stack-mobs.stack-radius"),
                config.getInt("stack-mobs.max-stack-size"),
                config.getString("stack-mobs.display-name"),
                config.getBoolean("stack-mobs.kill-all"),
                config.getBoolean("debug"),
                config.getBoolean("bloquear-mobs-nascerem"),
                config.getStringList("mundo-mobs"),
                config.getStringList("region-mobs"));
    }

    public static PluginConfigSnapshot empty() {
        return new PluginConfigSnapshot(
                1,
                16,
                200,
                200,
                1,
                true,
                5,
                1000,
                "&e{count}x &7{mob}",
                true,
                false,
                false,
                Collections.emptyList(),
                Collections.emptyList());
    }

    private static List<String> filterNonEmpty(List<String> source) {
        List<String> list = new ArrayList<>();
        if (source == null) {
            return list;
        }
        for (String value : source) {
            if (value != null && !value.trim().isEmpty()) {
                list.add(value.trim());
            }
        }
        return list;
    }

    private static String translate(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "§");
    }

    public String formatStackDisplayName(int count, EntityType type) {
        return this.stackMobsDisplayNameTemplate
                .replace("{count}", String.valueOf(count))
                .replace("{mob}", type.name());
    }

    public long nextSpawnDelay(Random random) {
        if (this.spawnersMaxDelay <= this.spawnersMinDelay) {
            return this.spawnersMinDelay;
        }
        int diff = this.spawnersMaxDelay - this.spawnersMinDelay;
        return this.spawnersMinDelay + random.nextInt(diff + 1);
    }

    public int getEngineTickInterval() {
        return this.engineTickInterval;
    }

    public int getActivationRange() {
        return this.activationRange;
    }

    public int getSpawnersSpawnCount() {
        return this.spawnersSpawnCount;
    }

    public int getStackMobsRadius() {
        return this.stackMobsRadius;
    }

    public int getStackMobsMaxStackSize() {
        return this.stackMobsMaxStackSize;
    }

    public boolean isStackMobsEnable() {
        return this.stackMobsEnable;
    }

    public boolean isStackMobsKillAll() {
        return this.stackMobsKillAll;
    }

    public boolean isDebug() {
        return this.debug;
    }

    public boolean isBloquearMobsNascerem() {
        return this.bloquearMobsNascerem;
    }

    public List<String> getBlockedWorlds() {
        return this.blockedWorlds;
    }

    public List<String> getBlockedRegions() {
        return this.blockedRegions;
    }
}
