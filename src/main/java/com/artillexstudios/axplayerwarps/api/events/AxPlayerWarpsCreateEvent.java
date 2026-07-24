package com.artillexstudios.axplayerwarps.api.events;

import com.artillexstudios.axplayerwarps.warps.Warp;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class AxPlayerWarpsCreateEvent extends Event {
    private static final HandlerList handlerList = new HandlerList();
    private final Player player;
    private final Warp warp;
    private final double creationPrice;

    public AxPlayerWarpsCreateEvent(Player player, Warp warp, double creationPrice) {
        super(!Bukkit.isPrimaryThread());

        this.player = player;
        this.warp = warp;
        this.creationPrice = creationPrice;
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

    public double getCreationPrice() {
        return creationPrice;
    }
}
