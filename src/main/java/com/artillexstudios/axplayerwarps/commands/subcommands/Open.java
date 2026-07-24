package com.artillexstudios.axplayerwarps.commands.subcommands;

import com.artillexstudios.axguiframework.actions.impl.ActionMenu;
import com.artillexstudios.axplayerwarps.warps.Warp;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import revxrsal.commands.exception.CommandErrorException;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.CONFIG;

public enum Open {
    INSTANCE;

    public void execute(CommandSender sender, @Nullable Warp warp) {
        if (warp != null) {
            if (!(sender instanceof Player pl)) throw new CommandErrorException("must-be-player");
            warp.teleportPlayer(pl);
            return;
        }
        execute(sender, (Player) null);
    }

    public void execute(CommandSender sender, @Nullable Player player) {
        Player openTo;
        if (player != null) {
            openTo = player;
        } else {
            if (sender instanceof Player pl) {
                openTo = pl;
            } else {
                throw new CommandErrorException("must-be-player");
            }
        }

        new ActionMenu().run(openTo, null, null, CONFIG.getString("default-gui", "categories"));
    }
}