package com.artillexstudios.axplayerwarps.user;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Users {
    private static final Map<Player, WarpUser> players = new ConcurrentHashMap<>();

    public static Map<Player, WarpUser> getPlayers() {
        return players;
    }

    public static void remove(Player player) {
        players.remove(player);
    }

    @NotNull
    public static WarpUser get(Player player) {
        return players.computeIfAbsent(player, WarpUser::new);
    }
}
