package com.github.jpmcodes.spawner;

import com.github.jpmcodes.spawner.commands.SetMobCommand;
import com.github.jpmcodes.spawner.commands.SpawnerCommand;
import com.github.jpmcodes.spawner.data.DatabaseProvider;
import com.github.jpmcodes.spawner.data.cache.CustomPlayerCache;
import com.github.jpmcodes.spawner.data.cache.PlayerSpawnerCache;
import com.github.jpmcodes.spawner.data.cache.SpawnerCache;
import com.github.jpmcodes.spawner.data.factory.SpawnerFactory;
import com.github.jpmcodes.spawner.data.storage.CustomPlayerStorage;
import com.github.jpmcodes.spawner.data.storage.PlayerSpawnerStorage;
import com.github.jpmcodes.spawner.listeners.GeneralListener;
import com.github.jpmcodes.spawner.listeners.MobRestricoesListener;
import com.github.jpmcodes.spawner.listeners.SpawnMobsListener;
import com.github.jpmcodes.spawner.tasks.SpawnerTask;
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
    private Configs savesConfig;

    // Database
    private DatabaseProvider databaseProvider;
    private CustomPlayerStorage customPlayerStorage;
    private PlayerSpawnerStorage playerSpawnerStorage;

    @Getter
    private static JSpawnerPlugin instance;

    @Override
    public void onEnable() {
        instance = this;

        databaseProvider = new DatabaseProvider(
                configs.getBoolean("database.enable"),
                configs.getString("database.data.host"),
                configs.getString("database.data.database"),
                configs.getString("database.data.username"),
                configs.getString("database.data.password"),
                configs.getInt("database.data.port"),
                getDataFolder()
        );
        databaseProvider.init();

        loadStorages();
        loadFactory();

        loadCommands();
        loadListeners();

        new SpawnerTask(this).runTaskTimer(this, 1L, Math.max(1L, getConfigs().getInt("engine-tick-interval")));
    }

    @Override
    public void onLoad() {
        loadConfigs();
        loadCache();
    }

    @Override
    public void onDisable() {
        if (customPlayerStorage != null) customPlayerStorage.saveAll();
        if (playerSpawnerStorage != null) playerSpawnerStorage.saveAll();

        if (databaseProvider != null) {
            databaseProvider.close();
        }
    }

    private void loadStorages() {
        customPlayerStorage = new CustomPlayerStorage(this, databaseProvider);
        customPlayerStorage.loadAll();

        playerSpawnerStorage = new PlayerSpawnerStorage(this, databaseProvider);
        playerSpawnerStorage.loadAll();
    }

    private void loadListeners() {
        getServer().getPluginManager().registerEvents(new GeneralListener(this), this);
        getServer().getPluginManager().registerEvents(new MobRestricoesListener(this), this);
        //getServer().getPluginManager().registerEvents(new SpawnMobsListener(this), this);
    }

    private void loadCommands() {
        getCommand("spawner").setExecutor(new SpawnerCommand(this));
        getCommand("setmob").setExecutor(new SetMobCommand(this));
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

        savesConfig = new Configs("saves.yml", this);
        if (!savesConfig.exists()) savesConfig.saveDefaultConfig();
    }
}