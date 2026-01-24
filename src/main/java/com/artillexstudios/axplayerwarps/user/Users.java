package com.artillexstudios.axplayerwarps.user;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Users {
    private static final ConcurrentHashMap<Player, WarpUser> players = new ConcurrentHashMap<>();

    public static Map<Player, WarpUser> getPlayers() {
        return players;
    }

    @NotNull
    public static WarpUser get(Player player) {
        return players.computeIfAbsent(player, WarpUser::new);
    }

    @NotNull
    public static WarpUser create(Player player) {
        WarpUser user = new WarpUser(player);
        WarpUser existing = players.putIfAbsent(player, user);
        return existing != null ? existing : user;
    }
}
