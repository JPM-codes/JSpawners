package com.github.jpmcodes.spawner;

import com.github.jpmcodes.spawner.commands.SpawnerCommand;
import com.github.jpmcodes.spawner.data.cache.CustomPlayerCache;
import com.github.jpmcodes.spawner.data.cache.PlayerSpawnerCache;
import com.github.jpmcodes.spawner.data.cache.SpawnerCache;
import com.github.jpmcodes.spawner.data.factory.SpawnerFactory;
import com.github.jpmcodes.spawner.listeners.GeneralListener;
import com.github.jpmcodes.spawner.utils.Configs;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
@Setter
public class JSpawnerPlugin extends JavaPlugin {

    // Caches
    private CustomPlayerCache customPlayerCache;
    private PlayerSpawnerCache playerSpawnerCache;
    private SpawnerCache spawnerCache;

    // Factories
    private SpawnerFactory spawnerFactory;

    // Configs
    private Configs spawnerConfig;
    private Configs messagesConfig;
    private Configs configs;

    @Getter
    private static JSpawnerPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        loadFactory();

        loadCommands();
        loadListeners();
    }

    @Override
    public void onLoad() {
        loadConfigs();
        loadCache();
    }

    private void loadListeners() {
        getServer().getPluginManager().registerEvents(new GeneralListener(this), this);
    }

    private void loadCommands() {
        getCommand("spawner").setExecutor(new SpawnerCommand(this));
    }

    private void loadFactory() {
        spawnerFactory = new SpawnerFactory(this);
        spawnerFactory.load();
    }

    private void loadCache() {
        spawnerCache = new SpawnerCache();
        playerSpawnerCache = new PlayerSpawnerCache();
        customPlayerCache = new CustomPlayerCache();
    }

    private void loadConfigs() {
        spawnerConfig = new Configs("spawners.yml", this);
        if (!spawnerConfig.exists()) spawnerConfig.saveDefaultConfig();

        messagesConfig = new Configs("messages.yml", this);
        if (!messagesConfig.exists()) messagesConfig.saveDefaultConfig();

        configs = new Configs("config.yml", this);
        if (!configs.exists()) configs.saveDefaultConfig();
    }
}
