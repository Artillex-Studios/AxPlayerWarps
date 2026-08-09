package com.artillexstudios.axplayerwarps.warps;

import org.bukkit.Location;

public record TeleportData(Warp warp, long date, Location location, String currency, double teleportPrice) {
}
