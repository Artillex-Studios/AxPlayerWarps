package com.artillexstudios.axplayerwarps.commands.shortcuts;

import com.artillexstudios.axplayerwarps.commands.annotations.AllWarps;
import com.artillexstudios.axplayerwarps.warps.Warp;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.DefaultFor;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.orphan.OrphanCommand;

/**
 * Standalone top-level version of {@code /pw warp}/{@code /pw go}, registered under
 * whatever names are configured in {@code subcommand-aliases.warp}.
 */
public class GoToWarpCommand implements OrphanCommand {

    @DefaultFor("~")
    @CommandPermission("axplayerwarps.use")
    public void warp(@NotNull Player sender, @AllWarps Warp warp) {
        warp.teleportPlayer(sender);
    }
}
