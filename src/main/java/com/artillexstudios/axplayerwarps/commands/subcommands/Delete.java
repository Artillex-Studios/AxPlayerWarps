package com.artillexstudios.axplayerwarps.commands.subcommands;

import com.artillexstudios.axplayerwarps.warps.Warp;
import org.bukkit.entity.Player;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.MESSAGEUTILS;

public enum Delete {
    INSTANCE;

    public void execute(Player sender, Warp warp) {
        if (!warp.getOwner().equals(sender.getUniqueId())) {
            MESSAGEUTILS.sendLang(sender, "errors.not-your-warp");
            return;
        }
        warp.delete();
    }
}