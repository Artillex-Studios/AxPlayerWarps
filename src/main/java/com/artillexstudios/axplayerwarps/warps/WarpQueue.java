package com.artillexstudios.axplayerwarps.warps;

import com.artillexstudios.axapi.scheduler.Scheduler;
import com.artillexstudios.axapi.utils.Cooldown;
import org.bukkit.entity.Player;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.CONFIG;
import static com.artillexstudios.axplayerwarps.AxPlayerWarps.MESSAGEUTILS;

public class WarpQueue {
    private static final Map<Player, TeleportData> queue = new ConcurrentHashMap<>();
    private static final Cooldown<Player> cooldown = Cooldown.create();

    public static void start() {
        Scheduler.get().runTimer(() -> {
            long time = CONFIG.getLong("teleport-delay-seconds");
            try {
                for (Iterator<Map.Entry<Player, TeleportData>> it = queue.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry<Player, TeleportData> e = it.next();
                    Player player = e.getKey();
                    if (cooldown.hasCooldown(player)) continue;
                    TeleportData teleportData = e.getValue();
                    Warp warp = teleportData.warp();

                    cooldown.addCooldown(e.getKey(), 1_000L);
                    long spent = (System.currentTimeMillis() - teleportData.date()) / 1_000L;
                    if (spent >= time) {
                        if (!warp.getLocation().equals(teleportData.location())
                                || warp.getTeleportPrice() != teleportData.teleportPrice()
                                || !Objects.equals(warp.getCurrency(), teleportData.currency())
                        ) {
                            MESSAGEUTILS.sendLang(player, "errors.warp-changed");
                        } else {
                            warp.completeTeleportPlayer(e.getKey());
                        }
                        it.remove();
                        continue;
                    }

                    MESSAGEUTILS.sendLang(player, "teleport.in", Map.of("%seconds%", "" + (time - spent)));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, 2, 2);
    }

    public static void addToQueue(Player player, Warp warp) {
        queue.put(player, new TeleportData(warp, System.currentTimeMillis(), warp.getLocation(), warp.getCurrency(), warp.getTeleportPrice()));
    }

    public static Map<Player, TeleportData> getQueue() {
        return queue;
    }

    public static void remove(Player player) {
        queue.remove(player);
    }
}
