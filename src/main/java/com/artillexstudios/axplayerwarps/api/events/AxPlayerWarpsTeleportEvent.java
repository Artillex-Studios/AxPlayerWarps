package com.artillexstudios.axplayerwarps.api.events;

import com.artillexstudios.axplayerwarps.warps.Warp;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class AxPlayerWarpsTeleportEvent extends Event {
    private static final HandlerList handlerList = new HandlerList();
    private final Player player;
    private final Warp warp;
    private final double teleportPrice;

    public AxPlayerWarpsTeleportEvent(Player player, Warp warp, double teleportPrice) {
        super(!Bukkit.isPrimaryThread());

        this.player = player;
        this.warp = warp;
        this.teleportPrice = teleportPrice;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    public Player getPlayer() {
        return player;
    }

    public Warp getWarp() {
        return warp;
    }

    public double getTeleportPrice() {
        return teleportPrice;
    }
}
