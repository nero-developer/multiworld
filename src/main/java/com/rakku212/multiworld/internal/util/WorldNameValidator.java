package com.rakku212.multiworld.internal.util;

import java.util.regex.Pattern;

public final class WorldNameValidator {

    private static final Pattern ALLOWED = Pattern.compile("^[a-zA-Z0-9._-]+$");

    private WorldNameValidator() {
    }

    public static boolean isValid(String name) {
        return name != null && !name.isBlank() && ALLOWED.matcher(name).matches() && name.length() <= 64;
    }
}
