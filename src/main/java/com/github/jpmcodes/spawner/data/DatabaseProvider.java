package com.github.jpmcodes.spawner.data;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DatabaseProvider {
    private final boolean useMySQL;
    private HikariDataSource hikari;
    private final String host;
    private final String database;


    private final String username;
    private final String password;
    private final int port;
    private final File SQLiteFile;

    public DatabaseProvider(boolean useMySQL, String host, String database, String username, String password, int port,
            File sqLiteFile) {
        this.useMySQL = useMySQL;
        this.host = host;
        this.database = database;
        this.username = username;
        this.password = password;
        this.port = port;
        this.SQLiteFile = new File(sqLiteFile, "spawners.db");
    }

    public void init() {
        try {
            HikariConfig config = new HikariConfig();

            if (this.useMySQL) {
                config.setJdbcUrl(
                        "jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database + "?useSSL=false");
                config.setUsername(this.username);
                config.setPassword(this.password);
                config.setMaximumPoolSize(10);
                config.setMinimumIdle(2);
            } else {
                if (!this.SQLiteFile.exists()) {
                    this.SQLiteFile.getParentFile().mkdirs();
                }
                config.setJdbcUrl("jdbc:sqlite:" + this.SQLiteFile.getAbsolutePath());
                config.setDriverClassName("org.sqlite.JDBC");
                config.setMaximumPoolSize(1);
            }

            config.setPoolName("Spawner");
            config.setConnectionTestQuery("SELECT 1");
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            this.hikari = new HikariDataSource(config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Connection getConnection() throws SQLException {
        if (this.hikari == null) {
            throw new SQLException("Database not initialized");
        }
        return this.hikari.getConnection();
    }

    public void close() {
        if (this.hikari != null && !this.hikari.isClosed())
            this.hikari.close();
    }
}
