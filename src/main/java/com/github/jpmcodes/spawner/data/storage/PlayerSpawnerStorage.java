package com.github.jpmcodes.spawner.data.storage;

import com.github.jpmcodes.spawner.JSpawnerPlugin;
import com.github.jpmcodes.spawner.data.DatabaseProvider;
import com.github.jpmcodes.spawner.data.cache.PlayerSpawnerCache;
import com.github.jpmcodes.spawner.data.models.CustomPlayer;
import com.github.jpmcodes.spawner.data.models.PlayerSpawnerModel;
import com.github.jpmcodes.spawner.data.models.SpawnerModel;
import com.github.jpmcodes.spawner.utils.LocationUtils;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class PlayerSpawnerStorage {

    private final JSpawnerPlugin plugin;
    private final DatabaseProvider database;

    public PlayerSpawnerStorage(JSpawnerPlugin plugin, DatabaseProvider database) {
        this.plugin = plugin;
        this.database = database;
        createTable();
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS player_spawners(player_uuid VARCHAR(36) NOT NULL, spawner_id VARCHAR(36) NOT NULL, spawner_location VARCHAR(255) PRIMARY KEY)";
        try (Connection conn = database.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadAll() {
        // Agora o loadAll() usa um delay para garantir que os mundos existam.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            String sql = "SELECT * FROM player_spawners";
            int count = 0;

            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    count++;
                    UUID playerUUID = UUID.fromString(resultSet.getString("player_uuid"));
                    String idSpawner = resultSet.getString("spawner_id");
                    String locationString = resultSet.getString("spawner_location");

                    // Busca ou cria o CustomPlayer no cache
                    CustomPlayer customPlayer = plugin.getCustomPlayerCache().getByUUID(playerUUID);
                    if (customPlayer == null) {
                        customPlayer = new CustomPlayer(playerUUID, "Unknown");
                        plugin.getCustomPlayerCache().addCachedElements(customPlayer);
                    }

                    // Busca ou cria o PlayerSpawnerModel no cache
                    PlayerSpawnerModel playerModel = plugin.getPlayerSpawnerCache().getByPlayerUUID(playerUUID);
                    if (playerModel == null) {
                        playerModel = new PlayerSpawnerModel(customPlayer, new ArrayList<>());
                        plugin.getPlayerSpawnerCache().addCachedElements(playerModel);
                    }

                    // Verifica se já não carregamos esse spawner (evitar duplicatas no cache)
                    final String finalLoc = locationString;
                    if (playerModel.getSpawners().stream().anyMatch(s -> LocationUtils.toString(s.getLocation()).equals(finalLoc))) {
                        continue;
                    }

                    SpawnerModel spawner = plugin.getSpawnerCache().getByID(idSpawner);
                    if (spawner == null) continue;

                    SpawnerModel spawnerClone = spawner.clone();
                    org.bukkit.Location loc = LocationUtils.fromString(locationString);
                    
                    if (loc == null || loc.getWorld() == null) {
                        continue;
                    }

                    spawnerClone.setLocation(loc);
                    playerModel.add(spawnerClone);
                }
                plugin.getLogger().info("[JSpawners] Carga de spawners concluida com sucesso (" + count + " verificados).");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, 40L); // 2 segundos de delay
    }

    public void loadForPlayer(CustomPlayer customPlayer) {
        String sql = "SELECT * FROM player_spawners WHERE player_uuid = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, customPlayer.getUuid().toString());
            
            try (ResultSet resultSet = statement.executeQuery()) {
                PlayerSpawnerModel playerModel = plugin.getPlayerSpawnerCache().getByPlayerUUID(customPlayer.getUuid());
                if (playerModel == null) {
                    playerModel = new PlayerSpawnerModel(customPlayer, new ArrayList<>());
                    plugin.getPlayerSpawnerCache().addCachedElements(playerModel);
                }

                while (resultSet.next()) {
                    String idSpawner = resultSet.getString("spawner_id");
                    String locationString = resultSet.getString("spawner_location");

                    // Evitar duplicados
                    final String finalLoc = locationString;
                    if (playerModel.getSpawners().stream().anyMatch(s -> LocationUtils.toString(s.getLocation()).equals(finalLoc))) {
                        continue;
                    }

                    SpawnerModel spawner = plugin.getSpawnerCache().getByID(idSpawner);
                    if (spawner == null) continue;

                    org.bukkit.Location loc = LocationUtils.fromString(locationString);
                    if (loc == null || loc.getWorld() == null) continue;

                    SpawnerModel spawnerClone = spawner.clone();
                    spawnerClone.setLocation(loc);
                    playerModel.add(spawnerClone);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveAll() {
        List<PlayerSpawnerModel> playerSpawners = plugin.getPlayerSpawnerCache().getCachedElements();
        for (PlayerSpawnerModel playerSpawner : playerSpawners) {
            save(playerSpawner);
        }
    }

    public void save(PlayerSpawnerModel playerSpawner) {
        String sql = "REPLACE INTO player_spawners (player_uuid, spawner_id, spawner_location) VALUES (?, ?, ?)";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (SpawnerModel spawner : playerSpawner.getSpawners()) {
                if (spawner.getLocation() == null || spawner.getLocation().getWorld() == null) continue;
                
                statement.setString(1, playerSpawner.getPlayer().getUuid().toString());
                statement.setString(2, spawner.getId());
                statement.setString(3, LocationUtils.toString(spawner.getLocation()));
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void save(PlayerSpawnerModel playerSpawner, SpawnerModel spawner) {
        if (spawner.getLocation() == null || spawner.getLocation().getWorld() == null) return;

        String sql = "REPLACE INTO player_spawners (player_uuid, spawner_id, spawner_location) VALUES (?, ?, ?)";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerSpawner.getPlayer().getUuid().toString());
            statement.setString(2, spawner.getId());
            statement.setString(3, LocationUtils.toString(spawner.getLocation()));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(PlayerSpawnerModel playerSpawner, SpawnerModel spawner) {
        String sql = "DELETE FROM player_spawners WHERE spawner_location = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, LocationUtils.toString(spawner.getLocation()));
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
