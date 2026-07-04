package com.github.jpmcodes.spawner;

import com.github.jpmcodes.spawner.data.DatabaseProvider;
import com.github.jpmcodes.spawner.data.cache.CustomPlayerCache;
import com.github.jpmcodes.spawner.data.cache.PlayerSpawnerCache;
import com.github.jpmcodes.spawner.data.cache.SpawnerCache;
import com.github.jpmcodes.spawner.data.factory.SpawnerFactory;
import com.github.jpmcodes.spawner.data.storage.CustomPlayerStorage;
import com.github.jpmcodes.spawner.data.storage.PlayerSpawnerStorage;
import com.github.jpmcodes.spawner.commands.SetMobCommand;
import com.github.jpmcodes.spawner.commands.SpawnerCommand;
import com.github.jpmcodes.spawner.config.PluginConfigCache;
import com.github.jpmcodes.spawner.listeners.GeneralListener;
import com.github.jpmcodes.spawner.listeners.MobRestricoesListener;
import com.github.jpmcodes.spawner.listeners.SpawnMobsListener;
import com.github.jpmcodes.spawner.tasks.SpawnerTask;
import com.github.jpmcodes.spawner.utils.Configs;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
@Setter
public class JSpawnerPlugin extends JavaPlugin {
    private CustomPlayerCache customPlayerCache;
    private PlayerSpawnerCache playerSpawnerCache;
    private SpawnerCache spawnerCache;
    private SpawnerFactory spawnerFactory;
    private Configs spawnerConfig;
    private Configs messagesConfig;

    private Configs configs;
    private Configs savesConfig;
    private DatabaseProvider databaseProvider;
    private CustomPlayerStorage customPlayerStorage;
    private PlayerSpawnerStorage playerSpawnerStorage;
    private PluginConfigCache configCache;
    
    @Getter
    private final java.util.Map<String, org.bukkit.Location> spawnLocationCache = new java.util.HashMap<>();

    @Getter
    private static JSpawnerPlugin instance;

    public void reloadSpawnLocationCache() {
        this.spawnLocationCache.clear();
        if (!this.savesConfig.contains("locais_nascimento")) {
            getLogger().info("[DEBUG] saves.yml não tem locais_nascimento");
            return;
        }
        org.bukkit.configuration.ConfigurationSection section = this.savesConfig.getSection("locais_nascimento");
        for (String playerUuid : section.getKeys(false)) {
            org.bukkit.configuration.ConfigurationSection playerSection = section.getConfigurationSection(playerUuid);
            for (String spawnerType : playerSection.getKeys(false)) {
                String path = "locais_nascimento." + playerUuid + "." + spawnerType;
                org.bukkit.Location loc = this.savesConfig.getLocation(path);
                if (loc != null && loc.getWorld() != null) {
                    this.spawnLocationCache.put(path, loc);
                }
            }
        }
        getLogger().info("[DEBUG] spawnLocationCache populado com " + spawnLocationCache.size() + " entradas");
    }


    public void onEnable() {
        instance = this;

        loadConfigs();
        this.configCache = new PluginConfigCache();
        this.configCache.reloadAll(this.configs, this.messagesConfig);
        loadCache();

        this.databaseProvider = new DatabaseProvider(this.configs.getBoolean("database.enable"),
                this.configs.getString("database.data.host"), this.configs.getString("database.data.database"),
                this.configs.getString("database.data.username"),
                this.configs.getString("database.data.password"), this.configs.getInt("database.data.port"),
                getDataFolder());

        this.databaseProvider.init();

        loadStorages();
        loadFactory();

        loadCommands();
        loadListeners();

        (new SpawnerTask(this)).runTaskTimer(this, 1L,
                this.configCache.getPlugin().getEngineTickInterval());

        Bukkit.getScheduler().runTaskLater(this, this::reloadSpawnLocationCache, 1L);
        // No onEnable:
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, this::desregistrarPlotSquaredSpawn, 1L);
    }

    public void onDisable() {
        removeStackedMobsFromWorlds();

        if (this.customPlayerStorage != null)
            this.customPlayerStorage.saveAll();
        if (this.playerSpawnerStorage != null)
            this.playerSpawnerStorage.saveAll();

        if (this.databaseProvider != null) {
            this.databaseProvider.close();
        }
    }

    private void removeStackedMobsFromWorlds() {
        int removed = 0;
        for (World world : getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.hasMetadata("stack-spawner") || entity.hasMetadata("spawner-id") || entity.hasMetadata("freeze")) {
                    entity.remove();
                    removed++;
                }
            }
        }
        getLogger().info("[JSpawners] Mobs stackados removidos ao desativar: " + removed);
    }

    private void loadStorages() {
        this.customPlayerStorage = new CustomPlayerStorage(this, this.databaseProvider);
        this.customPlayerStorage.loadAll();

        this.playerSpawnerStorage = new PlayerSpawnerStorage(this, this.databaseProvider);
        this.playerSpawnerStorage.loadAll();
    }

    private void loadListeners() {
        getServer().getPluginManager().registerEvents(new GeneralListener(this), this);
        getServer().getPluginManager().registerEvents(new MobRestricoesListener(this), this);
        getServer().getPluginManager().registerEvents(new SpawnMobsListener(this), this);
    }

    private void loadCommands() {
        getCommand("spawner").setExecutor(new SpawnerCommand(this));
        getCommand("setmob").setExecutor(new SetMobCommand(this));
    }

    private void loadFactory() {
        this.spawnerFactory = new SpawnerFactory(this);
        this.spawnerFactory.load();
    }

    private void loadCache() {
        this.spawnerCache = new SpawnerCache();
        this.playerSpawnerCache = new PlayerSpawnerCache();
        this.customPlayerCache = new CustomPlayerCache();
    }

    private void loadConfigs() {
        this.spawnerConfig = new Configs("spawners.yml", this);
        if (!this.spawnerConfig.exists())
            this.spawnerConfig.saveDefaultConfig();

        this.messagesConfig = new Configs("messages.yml", this);
        if (!this.messagesConfig.exists())
            this.messagesConfig.saveDefaultConfig();

        this.configs = new Configs("config.yml", this);
        if (!this.configs.exists())
            this.configs.saveDefaultConfig();

        this.savesConfig = new Configs("saves.yml", this);
        if (!this.savesConfig.exists())
            this.savesConfig.saveDefaultConfig();
    }

    public void desregistrarPlotSquaredSpawn() {
        HandlerList handlers = CreatureSpawnEvent.getHandlerList();
        for (RegisteredListener listener : handlers.getRegisteredListeners()) {
            Plugin p = listener.getPlugin();
            if (!p.getName().equalsIgnoreCase("PlotSquared")) continue;
            if (!listener.getListener().getClass().getSimpleName().equalsIgnoreCase("PlayerEvents")) continue;

            try {
                // Pega o método que está sendo chamado via reflection
                java.lang.reflect.Field executorField = RegisteredListener.class.getDeclaredField("executor");
                executorField.setAccessible(true);
                org.bukkit.plugin.EventExecutor originalExecutor = (org.bukkit.plugin.EventExecutor) executorField.get(listener);

                // Substitui por um executor que ignora o CreatureSpawnEvent
                org.bukkit.plugin.EventExecutor safeExecutor = (l, event) -> {
                    if (event instanceof CreatureSpawnEvent) return; // ignora silenciosamente
                    originalExecutor.execute(l, event);
                };

                executorField.set(listener, safeExecutor);
                this.getLogger().info("[JSpawners] MobSpawn do PlotSquared neutralizado com segurança.");
            } catch (Exception ex) {
                this.getLogger().warning("[JSpawners] Falha ao neutralizar PlotSquared: " + ex.getMessage());
            }
        }
    }
}