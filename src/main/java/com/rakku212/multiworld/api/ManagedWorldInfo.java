package com.rakku212.multiworld.api;

import org.bukkit.World;

/**
 * プラグインが SQLite に記録しているワールドのメタデータ。
 */
public record ManagedWorldInfo(
        String name,
        World.Environment environment,
        long seed,
        boolean loaded
) {
}
