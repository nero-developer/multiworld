package com.rakku212.multiworld.internal.util;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.bukkit.World;

public final class DimensionParser {

    private static final Map<String, World.Environment> ALIASES = Map.ofEntries(
            Map.entry("overworld", World.Environment.NORMAL),
            Map.entry("normal", World.Environment.NORMAL),
            Map.entry("default", World.Environment.NORMAL),
            Map.entry("nether", World.Environment.NETHER),
            Map.entry("hell", World.Environment.NETHER),
            Map.entry("end", World.Environment.THE_END),
            Map.entry("the_end", World.Environment.THE_END));

    private DimensionParser() {
    }

    public static Optional<World.Environment> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String key = raw.trim().toLowerCase(Locale.ROOT);
        if (ALIASES.containsKey(key)) {
            return Optional.of(ALIASES.get(key));
        }
        try {
            return Optional.of(World.Environment.valueOf(key.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
