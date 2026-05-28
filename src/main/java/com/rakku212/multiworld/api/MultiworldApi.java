package com.rakku212.multiworld.api;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * 他プラグイン向け公開 API。ワールド関連の呼び出しは基本的にメインスレッド前提です。
 */
public interface MultiworldApi {

    /**
     * 管理ワールドを非同期フローで作成する。完了時の {@link World} はメインスレッドで生成される。
     */
    CompletableFuture<World> create(World.Environment environment, String worldName, OptionalLong seed);

    /**
     * 作成完了を {@link CompletableFuture#whenComplete(BiConsumer)} と同じ契約でコールバックする。
     * <pre>{@code
     * api.create(env, name, seed, (world, error) -> {
     *     if (error != null) { ... return; }
     *     // world は null でない
     * });
     * }</pre>
     */
    default void create(
            World.Environment environment,
            String worldName,
            OptionalLong seed,
            BiConsumer<World, Throwable> whenComplete) {
        Objects.requireNonNull(whenComplete, "whenComplete");
        create(environment, worldName, seed).whenComplete(whenComplete);
    }

    /**
     * 成功時・失敗時をそれぞれラムダで渡す。
     * <pre>{@code
     * api.create(env, name, seed,
     *     world -> getLogger().info(world.getName()),
     *     Throwable::printStackTrace
     * );
     * }</pre>
     */
    default void create(
            World.Environment environment,
            String worldName,
            OptionalLong seed,
            Consumer<World> onSuccess,
            Consumer<Throwable> onFailure) {
        Objects.requireNonNull(onSuccess, "onSuccess");
        Objects.requireNonNull(onFailure, "onFailure");
        create(environment, worldName, seed).whenComplete((world, error) -> {
            if (error != null) {
                onFailure.accept(error);
            } else {
                onSuccess.accept(world);
            }
        });
    }

    /**
     * 成功時のみ処理する（失敗時は無視）。
     */
    default void create(
            World.Environment environment,
            String worldName,
            OptionalLong seed,
            Consumer<World> onSuccessOnly) {
        Objects.requireNonNull(onSuccessOnly, "onSuccessOnly");
        create(environment, worldName, seed, onSuccessOnly, t -> {});
    }

    /**
     * 登録済み管理ワールドをアンロードし、データベースから削除する。
     */
    boolean remove(String worldName);

    /**
     * 削除結果をコールバックで受け取る（{@link #remove(String)} と同じスレッドで実行される）。
     */
    default void remove(String worldName, Consumer<Boolean> whenDone) {
        Objects.requireNonNull(whenDone, "whenDone");
        whenDone.accept(remove(worldName));
    }

    /**
     * ロード済みの任意のワールド（バニラ既定ディメンション含む）のスポーンへプレイヤーを送る。
     */
    boolean teleport(Player player, String worldName);

    /**
     * テレポート成否をコールバックで受け取る（{@link #teleport(Player, String)} と同じスレッドで実行される）。
     */
    default void teleport(Player player, String worldName, Consumer<Boolean> whenDone) {
        Objects.requireNonNull(whenDone, "whenDone");
        whenDone.accept(teleport(player, worldName));
    }

    Optional<World> getLoadedWorld(String worldName);

    List<ManagedWorldInfo> listManagedWorlds();
}
