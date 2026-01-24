package com.artillexstudios.axplayerwarps.warps;

import com.artillexstudios.axapi.scheduler.Scheduler;
import com.artillexstudios.axapi.utils.Cooldown;
import com.artillexstudios.axapi.utils.Pair;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.CONFIG;
import static com.artillexstudios.axplayerwarps.AxPlayerWarps.MESSAGEUTILS;

public class WarpQueue {
    private static final ConcurrentHashMap<Player, Pair<Warp, Long>> queue = new ConcurrentHashMap<>();
    private static final Cooldown<Player> cooldown = Cooldown.create();

    public static void start() {
        Scheduler.get().runTimer(() -> {
            long time = CONFIG.getLong("teleport-delay-seconds");
            long currentTime = System.currentTimeMillis();

            queue.forEach((player, warpPair) -> {
                if (cooldown.hasCooldown(player)) return;
                cooldown.addCooldown(player, 1_000L);
                long spent = (currentTime - warpPair.getValue()) / 1_000L;
                if (spent >= time) {
                    if (queue.remove(player, warpPair)) {
                        warpPair.getKey().completeTeleportPlayer(player);
                    }
                } else {
                    MESSAGEUTILS.sendLang(player, "teleport.in", 
                        Map.of("%seconds%", String.valueOf(time - spent)));
                }
            });
        }, 2, 2);
    }

    public static void addToQueue(Player player, Warp warp) {
        queue.put(player, new Pair<>(warp, System.currentTimeMillis()));
    }

    public static Map<Player, Pair<Warp, Long>> getQueue() {
        return queue;
    }
}
