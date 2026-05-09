package com.artillexstudios.axplayerwarps.utils;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.CONFIG;

public class WarpNameUtils {
    public enum ValidationResult {
        ALLOWED,
        CONTAINS_SPACES,
        INVALID_LENGTH,
        DISALLOWED
    }

    public static ValidationResult isAllowed(String name) {
        if (!CONFIG.getBoolean("warp-naming.allow-spaces", false) && name.contains(" ")) {
            return ValidationResult.CONTAINS_SPACES;
        }

        if (name.length() < CONFIG.getInt("warp-naming.length.min", 1)
                || name.length() > CONFIG.getInt("warp-naming.length.max", 16)
        ) {
            return ValidationResult.INVALID_LENGTH;
        }

        if (CONFIG.getBoolean("warp-naming.allowed-characters.enabled", false)) {
            char[] list = CONFIG.getString("warp-naming.allowed-characters.list", "").toCharArray();
            for (char c1 : name.toCharArray()) {
                boolean found = false;
                for (char c2 : list) {
                    if (c1 != c2) continue;
                    found = true;
                    break;
                }
                if (!found) return ValidationResult.DISALLOWED;
            }
        }

        if (SimpleRegex.matches(CONFIG.getStringList("warp-naming.disallowed"), name, CONFIG.getBoolean("warp-naming.disallowed-ignore-case"))) {
            return ValidationResult.DISALLOWED;
        }

        return ValidationResult.ALLOWED;
    }
}
