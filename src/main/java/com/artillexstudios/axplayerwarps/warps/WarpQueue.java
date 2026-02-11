package com.artillexstudios.axplayerwarps.warps;

import com.artillexstudios.axapi.scheduler.Scheduler;
import com.artillexstudios.axapi.utils.Cooldown;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.CONFIG;
import static com.artillexstudios.axplayerwarps.AxPlayerWarps.MESSAGEUTILS;

public class WarpQueue {
    private static final ConcurrentHashMap<Player, TeleportData> queue = new ConcurrentHashMap<>();
    private static final Cooldown<Player> cooldown = Cooldown.create();

    public static void start() {
        Scheduler.get().runTimer(() -> {
            long time = CONFIG.getLong("teleport-delay-seconds");
            long currentTime = System.currentTimeMillis();

            queue.forEach((player, teleportData) -> {
                if (cooldown.hasCooldown(player)) return;
                cooldown.addCooldown(player, 1_000L);

                Warp warp = teleportData.warp();
                long spent = (currentTime - teleportData.date()) / 1_000L;

                if (spent >= time) {
                    if (queue.remove(player, teleportData)) {
                        if (!warp.getLocation().equals(teleportData.location())
                                || warp.getTeleportPrice() != teleportData.teleportPrice()
                                || !Objects.equals(warp.getCurrency(), teleportData.currency())) {
                            MESSAGEUTILS.sendLang(player, "errors.warp-changed");
                        } else {
                            warp.completeTeleportPlayer(player);
                        }
                    }
                } else {
                    MESSAGEUTILS.sendLang(player, "teleport.in", Map.of("%seconds%", String.valueOf(time - spent)));
                }
            });
        }, 2, 2);
    }

    public static void addToQueue(Player player, Warp warp) {
        queue.put(player, new TeleportData(warp, System.currentTimeMillis(), warp.getLocation(), warp.getCurrency(), warp.getTeleportPrice()));
    }

    public static Map<Player, TeleportData> getQueue() {
        return queue;
    }
}
