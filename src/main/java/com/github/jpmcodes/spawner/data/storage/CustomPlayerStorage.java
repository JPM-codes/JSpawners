package com.github.jpmcodes.spawner.data.storage;

import com.github.jpmcodes.spawner.JSpawnerPlugin;
import com.github.jpmcodes.spawner.data.DatabaseProvider;
import com.github.jpmcodes.spawner.data.models.CustomPlayer;
import com.github.jpmcodes.spawner.data.models.PlayerSpawnerModel;
import com.github.jpmcodes.spawner.data.models.SpawnerModel;
import com.github.jpmcodes.spawner.utils.LocationSerializer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class CustomPlayerStorage {

    private final JSpawnerPlugin plugin;
    private final DatabaseProvider database;

    public CustomPlayerStorage(JSpawnerPlugin plugin, DatabaseProvider database) {
        this.plugin = plugin;
        this.database = database;
        createTable();
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS players(player_uuid VARCHAR(36) PRIMARY KEY, player_name VARCHAR(255))";
        try (Connection conn = database.getConnection()) {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadAll() {
        String sql = "SELECT * FROM players";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                UUID playerUUID = UUID.fromString(resultSet.getString("player_uuid"));
                String playerName = resultSet.getString("player_name");

                plugin.getCustomPlayerCache().addCachedElements(
                        new CustomPlayer(
                                playerUUID,
                                playerName
                        )
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveAll() {
        List<CustomPlayer> players = plugin.getCustomPlayerCache().getCachedElements();

        if (players.isEmpty()) return;

        boolean useMySQL = database.isUseMySQL();

        String sql = useMySQL
                ? "INSERT INTO players (player_uuid, player_name) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE player_name = ?"
                : "REPLACE INTO players (player_uuid, player_name) VALUES (?, ?)";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            for (CustomPlayer player : players) {
                statement.setString(1, player.getUuid().toString());
                statement.setString(2, player.getName());

                if (useMySQL) {
                    statement.setString(3, player.getName());
                }

                statement.addBatch();
            }

            statement.executeBatch();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void save(CustomPlayer customPlayer) {
        boolean useMySQL = database.isUseMySQL();
        String sql = useMySQL
                ? "INSERT INTO players (player_uuid, player_name) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE player_name = ?"
                : "REPLACE INTO players (player_uuid, player_name) VALUES (?, ?)";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customPlayer.getUuid().toString());
            statement.setString(2, customPlayer.getName());
            if (useMySQL) {
                statement.setString(3, customPlayer.getName());
            }
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(CustomPlayer customPlayer) {
        String sql = "DELETE FROM players WHERE player_uuid = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, customPlayer.getUuid().toString());
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
