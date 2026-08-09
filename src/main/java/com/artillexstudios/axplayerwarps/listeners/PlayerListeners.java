package com.artillexstudios.axplayerwarps.listeners;

import com.artillexstudios.axplayerwarps.user.Users;
import com.artillexstudios.axplayerwarps.warps.WarpQueue;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListeners implements Listener {
    public PlayerListeners() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Users.get(player);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Users.get(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Users.remove(event.getPlayer());
        WarpQueue.remove(event.getPlayer());
    }
}
