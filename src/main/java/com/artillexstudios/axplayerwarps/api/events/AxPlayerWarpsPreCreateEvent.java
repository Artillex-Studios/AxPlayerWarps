package com.artillexstudios.axplayerwarps.api.events;

import com.artillexstudios.axplayerwarps.warps.Warp;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class AxPlayerWarpsPreCreateEvent extends Event implements Cancellable {
    private static final HandlerList handlerList = new HandlerList();
    private final Player player;
    private final Warp warp;
    private boolean cancelled;
    private double creationPrice;

    public AxPlayerWarpsPreCreateEvent(Player player, Warp warp, double creationPrice) {
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

    public void setCreationPrice(double creationPrice) {
        this.creationPrice = creationPrice;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
