package com.artillexstudios.axplayerwarps.sorting;

import com.artillexstudios.axplayerwarps.warps.Warp;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

public class WarpComparator implements Comparator<Warp> {
    private final Sort sort;
    private final Player player;

    public WarpComparator(Sort sort, Player player) {
        this.sort = sort;
        this.player = player;
    }

    @Override
    public int compare(@NotNull Warp w1, @NotNull Warp w2) {
        return switch (sort.sorting()) {
            case ALPHABETICAL -> w1.getName().compareTo(w2.getName());
            case VISITS -> Integer.compare(w1.getUniqueVisits(), w2.getUniqueVisits());
            case RATING -> Float.compare(w1.getRating(), w2.getRating());
            case RATING_COUNT -> Integer.compare(w1.getRatingAmount(), w2.getRatingAmount());
            case FAVORITES -> Integer.compare(w1.getFavorites(), w2.getFavorites());
            case DISTANCE -> Double.compare(distance(w1), distance(w2));
            case CREATION_DATE -> Long.compare(w1.getCreated(), w2.getCreated());
        } * (sort.reverse() ? -1 : 1);
    }

    private double distance(Warp warp) {
        if (!player.getWorld().equals(warp.getLocation().getWorld())) return Double.MAX_VALUE;
        return player.getLocation().distanceSquared(warp.getLocation());
    }
}
