package com.github.jpmcodes.spawner.utils;

import com.github.jpmcodes.spawner.JSpawnerPlugin;

public class Debug {
    public static void info(String message) {
        if (JSpawnerPlugin.getInstance().getConfigCache().getPlugin().isDebug())
            JSpawnerPlugin.getInstance().getLogger().info(message);
    }

    public static void warning(String message) {
        if (JSpawnerPlugin.getInstance().getConfigCache().getPlugin().isDebug())
            JSpawnerPlugin.getInstance().getLogger().warning(message);
    }

    public static void error(String message) {
        if (JSpawnerPlugin.getInstance().getConfigCache().getPlugin().isDebug())
            JSpawnerPlugin.getInstance().getLogger().severe(message);
    }
}