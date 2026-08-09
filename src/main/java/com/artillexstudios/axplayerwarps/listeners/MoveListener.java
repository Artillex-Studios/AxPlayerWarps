package com.artillexstudios.axplayerwarps.listeners;

import com.artillexstudios.axplayerwarps.warps.WarpQueue;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.MESSAGEUTILS;

public class MoveListener implements Listener {

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        if (!WarpQueue.getQueue().containsKey(event.getPlayer())) return;
        // using distanceSquared to avoid heavy Math.sqrt call
        if (event.getFrom().distanceSquared(event.getTo()) < 0.005) return;
        WarpQueue.remove(event.getPlayer());
        MESSAGEUTILS.sendLang(event.getPlayer(), "errors.moved");
    }
}
