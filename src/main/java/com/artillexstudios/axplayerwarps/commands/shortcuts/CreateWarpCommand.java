package com.artillexstudios.axplayerwarps.commands.shortcuts;

import com.artillexstudios.axplayerwarps.commands.subcommands.Create;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.DefaultFor;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.orphan.OrphanCommand;

/**
 * Standalone top-level version of {@code /pw create}/{@code /pw set}, registered under
 * whatever names are configured in {@code subcommand-aliases.create}.
 */
public class CreateWarpCommand implements OrphanCommand {

    @DefaultFor("~")
    @CommandPermission("axplayerwarps.create")
    public void create(@NotNull Player sender, String warpName) {
        Create.INSTANCE.execute(sender, warpName);
    }
}
