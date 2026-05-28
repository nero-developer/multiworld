package com.rakku212.multiworld.internal.service;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.rakku212.multiworld.api.ManagedWorldInfo;
import com.rakku212.multiworld.api.MultiworldApi;

public final class MultiworldApiImpl implements MultiworldApi {

    private final JavaPlugin plugin;
    private final MultiworldService service;

    public MultiworldApiImpl(JavaPlugin plugin, MultiworldService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @Override
    public CompletableFuture<World> create(
            World.Environment environment,
            String worldName,
            OptionalLong seed) {
        return service.createManagedWorldAsync(environment, worldName, seed);
    }

    @Override
    public boolean remove(String worldName) {
        if (Bukkit.isPrimaryThread()) {
            return service.removeManagedWorld(worldName);
        }
        CompletableFuture<Boolean> done = new CompletableFuture<>();
        plugin.getServer().getScheduler().runTask(plugin, () -> done.complete(service.removeManagedWorld(worldName)));
        try {
            return done.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException ignored) {
            return false;
        }
    }

    @Override
    public boolean teleport(Player player, String worldName) {
        if (player == null) {
            return false;
        }
        if (Bukkit.isPrimaryThread()) {
            return service.teleportPlayer(player, worldName);
        }
        CompletableFuture<Boolean> done = new CompletableFuture<>();
        plugin.getServer().getScheduler().runTask(plugin, () -> done.complete(service.teleportPlayer(player, worldName)));
        try {
            return done.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException ignored) {
            return false;
        }
    }

    @Override
    public Optional<World> getLoadedWorld(String worldName) {
        return service.getLoadedWorld(worldName);
    }

    @Override
    public List<ManagedWorldInfo> listManagedWorlds() {
        return service.listManagedWorlds();
    }
}
