package com.artillexstudios.axplayerwarps.commands;

import com.artillexstudios.axplayerwarps.AxPlayerWarps;
import com.artillexstudios.axplayerwarps.commands.annotations.AllWarps;
import com.artillexstudios.axplayerwarps.commands.annotations.OwnWarps;
import com.artillexstudios.axplayerwarps.utils.CommandMessages;
import com.artillexstudios.axplayerwarps.warps.Warp;
import com.artillexstudios.axplayerwarps.warps.WarpManager;
import revxrsal.commands.bukkit.BukkitCommandActor;
import revxrsal.commands.bukkit.BukkitCommandHandler;
import revxrsal.commands.exception.CommandErrorException;
import revxrsal.commands.orphan.Orphans;

import java.util.Locale;
import java.util.Map;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.CONFIG;
import static com.artillexstudios.axplayerwarps.AxPlayerWarps.MESSAGEUTILS;

public class CommandManager {
    private static BukkitCommandHandler handler = null;

    public static void load() {
        handler = BukkitCommandHandler.create(AxPlayerWarps.getInstance());

        handler.getTranslator().add(new CommandMessages());
        handler.setLocale(Locale.of("en", "US"));

        handler.getAutoCompleter().registerSuggestionFactory(parameter -> {
            if (parameter.hasAnnotation(AllWarps.class)) {
                return (args, sender, command) -> {
                    return WarpManager.getWarps().stream().map(Warp::getName).toList();
                };
            }
            if (parameter.hasAnnotation(OwnWarps.class)) {
                return (args, sender, command) -> {
                    return WarpManager.getWarps(sender.getUniqueId()).stream().map(Warp::getName).toList();
                };
            }
            return null;
        });

        handler.registerValueResolver(Warp.class, resolver -> {
            String name = resolver.popForParameter();
            Warp warp = WarpManager.getWarp(name);
            if (warp == null) {
                MESSAGEUTILS.sendLang(resolver.actor().as(BukkitCommandActor.class).getSender(), "errors.not-found", Map.of("%warp%", name));
                throw new CommandErrorException();
            }
            return warp;
        });

        reload();
    }

    public static void reload() {
        handler.unregisterAllCommands();

        handler.register(Orphans.path(CONFIG.getStringList("main-command-aliases").toArray(String[]::new)).handler(new MainCommand()));
        handler.register(Orphans.path(CONFIG.getStringList("admin-command-aliases").toArray(String[]::new)).handler(new AdminCommand()));

        handler.registerBrigadier();
    }
}
