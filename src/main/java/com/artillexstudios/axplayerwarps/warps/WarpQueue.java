package com.artillexstudios.axplayerwarps.warps;

import com.artillexstudios.axapi.scheduler.Scheduler;
import com.artillexstudios.axapi.utils.Cooldown;
import com.artillexstudios.axapi.utils.Pair;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.CONFIG;
import static com.artillexstudios.axplayerwarps.AxPlayerWarps.MESSAGEUTILS;

public class WarpQueue {
    private static final Map<Player, Pair<Warp, Long>> queue = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Cooldown<Player> cooldown = new Cooldown<>();

    public static void start() {
        Scheduler.get().runTimer(() -> {
            long time = CONFIG.getLong("teleport-delay-seconds");
            try {
                synchronized (queue) {
                    for (Iterator<Map.Entry<Player, Pair<Warp, Long>>> it = queue.entrySet().iterator(); it.hasNext(); ) {
                        Map.Entry<Player, Pair<Warp, Long>> e = it.next();
                        if (cooldown.hasCooldown(e.getKey())) continue;

                        cooldown.addCooldown(e.getKey(), 1_000L);
                        long spent = (System.currentTimeMillis() - e.getValue().getValue()) / 1_000L;
                        if (spent >= time) {
                            e.getValue().getKey().completeTeleportPlayer(e.getKey());
                            it.remove();
                            continue;
                        }

                        MESSAGEUTILS.sendLang(e.getKey(), "teleport.in", Map.of("%seconds%", "" + (time - spent)));
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, 2, 2);
    }

    public static void addToQueue(Player player, Warp warp) {
        queue.put(player, new Pair<>(warp, System.currentTimeMillis()));
    }

    public static Map<Player, Pair<Warp, Long>> getQueue() {
        return queue;
    }
}
