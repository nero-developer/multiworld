package com.rakku212.multiworld.internal.persistence;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.bukkit.World;

import com.rakku212.multiworld.api.ManagedWorldInfo;

public final class ManagedWorldRepository {

    private final DataSource dataSource;

    public ManagedWorldRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean existsByName(String name) {
        try (var conn = dataSource.getConnection();
                var ps = conn.prepareStatement("SELECT 1 FROM managed_worlds WHERE name = ? LIMIT 1")) {
            ps.setString(1, name);
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public void insert(String name, World.Environment environment, long seed) {
        try (var conn = dataSource.getConnection();
                var ps = conn.prepareStatement(
                        "INSERT INTO managed_worlds (name, environment, seed) VALUES (?, ?, ?)")) {
            ps.setString(1, name);
            ps.setString(2, environment.name());
            ps.setLong(3, seed);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean delete(String name) {
        try (var conn = dataSource.getConnection();
                var ps = conn.prepareStatement("DELETE FROM managed_worlds WHERE name = ?")) {
            ps.setString(1, name);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<ManagedWorldInfo> findAll(org.bukkit.Server server) {
        List<ManagedRow> rows = findAllRows();
        List<ManagedWorldInfo> list = new ArrayList<>(rows.size());
        for (ManagedRow row : rows) {
            var world = server.getWorld(row.name());
            list.add(new ManagedWorldInfo(row.name(), row.environment(), row.seed(), world != null));
        }
        return list;
    }

    public List<ManagedRow> findAllRows() {
        try (var conn = dataSource.getConnection();
                var ps = conn.prepareStatement("SELECT name, environment, seed FROM managed_worlds ORDER BY name");
                var rs = ps.executeQuery()) {
            List<ManagedRow> list = new ArrayList<>();
            while (rs.next()) {
                String name = rs.getString("name");
                World.Environment env = World.Environment.valueOf(rs.getString("environment"));
                long seed = rs.getLong("seed");
                list.add(new ManagedRow(name, env, seed));
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public Optional<ManagedRow> findByName(String name) {
        try (var conn = dataSource.getConnection();
                var ps = conn.prepareStatement(
                        "SELECT name, environment, seed FROM managed_worlds WHERE name = ? LIMIT 1")) {
            ps.setString(1, name);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new ManagedRow(
                        rs.getString("name"),
                        World.Environment.valueOf(rs.getString("environment")),
                        rs.getLong("seed")));
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public record ManagedRow(String name, World.Environment environment, long seed) {
    }
}
