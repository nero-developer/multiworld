package com.rakku212.multiworld.internal.persistence;

import java.io.File;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.bukkit.plugin.java.JavaPlugin;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public final class DatabaseManager implements AutoCloseable {

    private final HikariDataSource dataSource;

    public DatabaseManager(JavaPlugin plugin) {
        File dbFile = new File(plugin.getDataFolder(), "multiworld.db");
        plugin.getDataFolder().mkdirs();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(4);
        config.setPoolName("multiworld-sqlite");
        this.dataSource = new HikariDataSource(config);

        migrate();
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    private void migrate() {
        try (var conn = dataSource.getConnection();
                var st = conn.createStatement()) {
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS managed_worlds (
                        name TEXT NOT NULL PRIMARY KEY,
                        environment TEXT NOT NULL,
                        seed INTEGER NOT NULL
                    )
                    """);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to migrate SQLite schema", e);
        }
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
