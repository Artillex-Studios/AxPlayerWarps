package com.artillexstudios.axplayerwarps.warps;

import com.artillexstudios.axplayerwarps.AxPlayerWarps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
}
