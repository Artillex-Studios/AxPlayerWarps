package com.artillexstudios.axplayerwarps.warps;

import com.artillexstudios.axplayerwarps.AxPlayerWarps;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class WarpManager {
    private static final List<Warp> warps = Collections.synchronizedList(new ArrayList<>());

    public static void load() {
        AxPlayerWarps.getThreadedQueue().submit(() -> {
            AxPlayerWarps.getDatabase().loadWarps();
        });
    }

    public static List<Warp> getWarps() {
        return warps;
    }

    public static List<Warp> getWarps(OfflinePlayer offlinePlayer) {
        return getWarps(offlinePlayer.getUniqueId());
    }

    public static List<Warp> getWarps(UUID uuid) {
        List<Warp> collected = new ArrayList<>();
        for (Warp warp : warps) {
            if (!warp.getOwner().equals(uuid)) continue;
            collected.add(warp);
        }
        return collected;
    }

    @Nullable
    public static Warp getWarp(String name) {
        return getWarp(name, false);
    }

    @Nullable
    public static Warp getWarp(String name, boolean caseSensitive) {
        Warp matching = null; // first look for exact matches, if none are found, then insensitive matches are also good
        for (Warp warp : warps) {
            if (warp.getName().equals(name)) {
                return warp;
            }
            if (caseSensitive) continue;
            if (!warp.getName().equalsIgnoreCase(name)) continue;
            matching = warp;
        }
        return matching;
    }
}
