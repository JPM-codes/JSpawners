package com.github.jpmcodes.spawner.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Getter
@Setter
public class DatabaseProvider {
    private final boolean useMySQL;
    private HikariDataSource hikari;

    private final String host, database, username, password;
    private final int port;
    private final File SQLiteFile;

    public DatabaseProvider(boolean useMySQL, String host, String database, String username, String password, int port, File sqLiteFile) {
        this.useMySQL = useMySQL;
        this.host = host;
        this.database = database;
        this.username = username;
        this.password = password;
        this.port = port;
        SQLiteFile = new File(sqLiteFile, "spawners.db");
    }

    public void init() {
        try {
            HikariConfig config = new HikariConfig();
            
            if (useMySQL) {
                config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false");
                config.setUsername(username);
                config.setPassword(password);
                config.setMaximumPoolSize(10);
                config.setMinimumIdle(2);
            } else {
                if (!SQLiteFile.exists()) {
                    SQLiteFile.getParentFile().mkdirs();
                }
                config.setJdbcUrl("jdbc:sqlite:" + SQLiteFile.getAbsolutePath());
                config.setDriverClassName("org.sqlite.JDBC");
                config.setMaximumPoolSize(1);
            }
            
            config.setPoolName("Spawner");
            config.setConnectionTestQuery("SELECT 1");
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            hikari = new HikariDataSource(config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        if (hikari == null) {
            throw new SQLException("Database not initialized");
        }
        return hikari.getConnection();
    }

    public void close() {
        if (hikari != null && !hikari.isClosed()) {
            hikari.close();
        }
    }

}
