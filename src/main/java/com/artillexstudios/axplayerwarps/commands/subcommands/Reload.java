package com.artillexstudios.axplayerwarps.commands.subcommands;

import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axplayerwarps.category.CategoryManager;
import com.artillexstudios.axplayerwarps.commands.MainCommand;
import com.artillexstudios.axplayerwarps.guis.impl.CategoryGui;
import com.artillexstudios.axplayerwarps.guis.impl.WarpsGui;
import com.artillexstudios.axplayerwarps.hooks.HookManager;
import com.artillexstudios.axplayerwarps.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.Map;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.CONFIG;
import static com.artillexstudios.axplayerwarps.AxPlayerWarps.CURRENCIES;
import static com.artillexstudios.axplayerwarps.AxPlayerWarps.LANG;
import static com.artillexstudios.axplayerwarps.AxPlayerWarps.MESSAGEUTILS;

public enum Reload {
    INSTANCE;

    public void execute(CommandSender sender) {
        Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33EEBB[AxPlayerWarps] &#99FFDDReloading configuration..."));
        if (!CONFIG.reload()) {
            MESSAGEUTILS.sendFormatted(sender, "reload.failed", Map.of("%file%", "config.yml"));
            return;
        }
        Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33EEBB╠ &#99FFDDReloaded &fconfig.yml&#99FFDD!"));

        if (!LANG.reload()) {
            MESSAGEUTILS.sendFormatted(sender, "reload.failed", Map.of("%file%", "lang.yml"));
            return;
        }
        Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33EEBB╠ &#99FFDDReloaded &flang.yml&#99FFDD!"));

        if (!CURRENCIES.reload()) {
            MESSAGEUTILS.sendFormatted(sender, "reload.failed", Map.of("%file%", "currencies.yml"));
            return;
        }
        Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33EEBB╠ &#99FFDDReloaded &fcurrencies.yml&#99FFDD!"));

        if (!CategoryGui.reload()) {
            MESSAGEUTILS.sendFormatted(sender, "reload.failed", Map.of("%file%", "guis/categories.yml"));
            return;
        }
        Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33EEBB╠ &#99FFDDReloaded &fguis/categories.yml&#99FFDD!"));

        if (!WarpsGui.reload()) {
            MESSAGEUTILS.sendFormatted(sender, "reload.failed", Map.of("%file%", "guis/warps.yml"));
            return;
        }
        Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33EEBB╠ &#99FFDDReloaded &fguis/warps.yml&#99FFDD!"));

        HookManager.updateHooks();
        WorldManager.reload();
        CategoryManager.reload();
        MainCommand.registerCommand();

        Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33EEBB╚ &#99FFDDSuccessful reload!"));
        MESSAGEUTILS.sendLang(sender, "reload.success");
    }
}