package com.artillexstudios.axplayerwarps.commands.shortcuts;

import com.artillexstudios.axplayerwarps.commands.annotations.OwnWarps;
import com.artillexstudios.axplayerwarps.commands.subcommands.Edit;
import com.artillexstudios.axplayerwarps.warps.Warp;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.DefaultFor;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.orphan.OrphanCommand;

/**
 * Standalone top-level version of {@code /pw edit}/{@code /pw settings}, registered under
 * whatever names are configured in {@code subcommand-aliases.edit}.
 */
public class EditWarpCommand implements OrphanCommand {

    @DefaultFor("~")
    @CommandPermission("axplayerwarps.edit")
    public void edit(@NotNull Player sender, @OwnWarps Warp warp) {
        Edit.INSTANCE.execute(sender, warp);
    }
}
