package com.rakku212.multiworld.internal.service;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.rakku212.multiworld.api.ManagedWorldInfo;
import com.rakku212.multiworld.internal.persistence.ManagedWorldRepository;
import com.rakku212.multiworld.internal.util.WorldNameValidator;

public final class MultiworldService {

    private final JavaPlugin plugin;
    private final ManagedWorldRepository repository;

    public MultiworldService(JavaPlugin plugin, ManagedWorldRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public void loadStoredWorlds() {
        for (ManagedWorldRepository.ManagedRow row : repository.findAllRows()) {
            if (Bukkit.getWorld(row.name()) != null) {
                continue;
            }
            File folder = new File(Bukkit.getWorldContainer(), row.name());
            if (!folder.isDirectory()) {
                plugin.getLogger().warning("管理ワールド \"" + row.name() + "\" のフォルダが見つかりません。スキップします。");
                continue;
            }
            WorldCreator creator = new WorldCreator(row.name());
            creator.environment(row.environment());
            creator.seed(row.seed());
            World w = creator.createWorld();
            if (w == null) {
                plugin.getLogger().severe("管理ワールド \"" + row.name() + "\" の読み込みに失敗しました。");
            }
        }
    }

    public CompletableFuture<World> createManagedWorldAsync(
            World.Environment environment,
            String worldName,
            OptionalLong seed) {
        CompletableFuture<World> result = new CompletableFuture<>();
        Executor async = r -> plugin.getServer().getScheduler().runTaskAsynchronously(plugin, r);

        async.execute(() -> {
            try {
                if (!WorldNameValidator.isValid(worldName)) {
                    result.completeExceptionally(new IllegalArgumentException("ワールド名が不正です。"));
                    return;
                }
                if (repository.existsByName(worldName)) {
                    result.completeExceptionally(new IllegalStateException("その名前は既にデータベースに登録されています。"));
                    return;
                }
                if (new File(Bukkit.getWorldContainer(), worldName).exists()) {
                    result.completeExceptionally(new IllegalStateException("同名のワールドフォルダが既に存在します。"));
                    return;
                }
                if (Bukkit.getWorld(worldName) != null) {
                    result.completeExceptionally(new IllegalStateException("同名のワールドが既にロードされています。"));
                    return;
                }

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    try {
                        WorldCreator creator = new WorldCreator(worldName);
                        creator.environment(environment);
                        seed.ifPresent(creator::seed);
                        World world = creator.createWorld();
                        if (world == null) {
                            result.completeExceptionally(new IllegalStateException("ワールドの作成に失敗しました。"));
                            return;
                        }
                        repository.insert(worldName, environment, world.getSeed());
                        result.complete(world);
                    } catch (Exception e) {
                        result.completeExceptionally(e);
                    }
                });
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        });
        return result;
    }

    /**
     * メインスレッドから呼び出す。
     */
    public boolean removeManagedWorld(String worldName) {
        Optional<ManagedWorldRepository.ManagedRow> row = repository.findByName(worldName);
        if (row.isEmpty()) {
            return false;
        }
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            if (!Bukkit.unloadWorld(world, true)) {
                plugin.getLogger().warning("ワールド \"" + worldName + "\" をアンロードできませんでした。");
                return false;
            }
        }
        return repository.delete(worldName);
    }

    public Optional<World> getLoadedWorld(String worldName) {
        return Optional.ofNullable(Bukkit.getWorld(worldName));
    }

    public List<ManagedWorldInfo> listManagedWorlds() {
        return repository.findAll(plugin.getServer());
    }

    public boolean isManaged(String worldName) {
        return repository.existsByName(worldName);
    }

    /**
     * メインスレッドから呼び出す。
     */
    public boolean teleportPlayer(Player player, String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return false;
        }
        return player.teleport(world.getSpawnLocation());
    }
}
