package com.artillexstudios.axplayerwarps.commands.subcommands;

import com.artillexstudios.axplayerwarps.guis.EditWarpGui;
import com.artillexstudios.axplayerwarps.warps.Warp;
import org.bukkit.entity.Player;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.MESSAGEUTILS;

public enum Edit {
    INSTANCE;

    public void execute(Player sender, Warp warp) {
        if (!warp.getOwner().equals(sender.getUniqueId())) {
            MESSAGEUTILS.sendLang(sender, "errors.not-your-warp");
            return;
        }
        new EditWarpGui(sender, warp).open();
    }
}