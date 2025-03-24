package com.artillexstudios.axplayerwarps.guis.impl;

import com.artillexstudios.axapi.config.Config;
import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.settings.dumper.DumperSettings;
import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.settings.general.GeneralSettings;
import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.settings.loader.LoaderSettings;
import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.settings.updater.UpdaterSettings;
import com.artillexstudios.axapi.nms.NMSHandlers;
import com.artillexstudios.axapi.scheduler.Scheduler;
import com.artillexstudios.axapi.utils.ItemBuilder;
import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axplayerwarps.AxPlayerWarps;
import com.artillexstudios.axplayerwarps.database.impl.Base;
import com.artillexstudios.axplayerwarps.enums.AccessList;
import com.artillexstudios.axplayerwarps.guis.GuiFrame;
import com.artillexstudios.axplayerwarps.guis.actions.Actions;
import com.artillexstudios.axplayerwarps.input.InputManager;
import com.artillexstudios.axplayerwarps.placeholders.Placeholders;
import com.artillexstudios.axplayerwarps.warps.Warp;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.MESSAGEUTILS;

public class WhitelistGui extends GuiFrame {
    private static final Config GUI = new Config(new File(AxPlayerWarps.getInstance().getDataFolder(), "guis/whitelist.yml"),
            AxPlayerWarps.getInstance().getResource("guis/whitelist.yml"),
            GeneralSettings.builder().setUseDefaults(false).build(),
            LoaderSettings.builder().build(),
            DumperSettings.DEFAULT,
            UpdaterSettings.builder().build()
    );

    private final PaginatedGui gui;
    private final Warp warp;
    private final GuiFrame lastGui;
    private static final AccessList al = AccessList.WHITELIST;

    public WhitelistGui(Player player, Warp warp, GuiFrame lastGui) {
        super(GUI, player);
        this.warp = warp;
        this.gui = Gui.paginated()
            .disableAllInteractions()
            .title(StringUtils.format(GUI.getString("title", ""), Map.of("%warp%", warp.getName())))
            .rows(GUI.getInt("rows", 5))
            .pageSize(GUI.getInt("page-size", 21))
            .create();
        this.lastGui = lastGui;

        setWarp(warp);
        setGui(gui);
    }

    public static boolean reload() {
        return GUI.reload();
    }

    public void open() {
        createItem("add", event -> {
            Actions.run(player, this, file.getStringList("add.actions"));
            if (event.isRightClick() && event.isShiftClick()) {
                AxPlayerWarps.getThreadedQueue().submit(() -> {
                    AxPlayerWarps.getDatabase().clearList(warp, al);
                    MESSAGEUTILS.sendLang(player, al.name().toLowerCase() + ".clear");
                    open();
                });
                return;
            }
            InputManager.getInput(player, "add-player", result -> {
                if (result.equalsIgnoreCase(player.getName())) {
                    MESSAGEUTILS.sendLang(player, "errors." + al.name().toLowerCase() + "-self");
                    open();
                    return;
                }
                AxPlayerWarps.getThreadedQueue().submit(() -> {
                    UUID uuid = AxPlayerWarps.getDatabase().getUUIDFromName(result);
                    if (uuid == null) {
                        MESSAGEUTILS.sendLang(player, "errors.player-not-found");
                    } else {
                        AxPlayerWarps.getDatabase().addToList(warp, al, Bukkit.getOfflinePlayer(uuid));
                        MESSAGEUTILS.sendLang(player, al.name().toLowerCase() + ".add", Map.of("%player%", result));
                    }
                    Scheduler.get().runAt(player.getLocation(), this::open);
                });
            });
        }, Map.of());

        load().thenRun(() -> {
            updateTitle();
            gui.open(player);
        });
    }

    public CompletableFuture<Void> load() {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        AxPlayerWarps.getThreadedQueue().submit(() -> {
            gui.clearPageItems();
            for (Base.AccessPlayer accessPlayer : warp.getAccessList(al)) {
                ItemBuilder builder = new ItemBuilder(file.getSection(al.getRoute()));
                if (builder.get().getType() == Material.PLAYER_HEAD) {
                    final Player pl = Bukkit.getPlayer(warp.getOwner());
                    if (pl != null) {
                        var textures = NMSHandlers.getNmsHandler().textures(pl);
                        if (textures != null) builder.setTextureValue(textures.getKey());
                    }
                }

                builder.setName(Placeholders.parse(accessPlayer, player, GUI.getString(al.getRoute() + ".name")));
                builder.setLore(Placeholders.parseList(accessPlayer, player, GUI.getStringList(al.getRoute() + ".lore")));

                gui.addItem(new GuiItem(builder.get(), event -> {
                    AxPlayerWarps.getThreadedQueue().submit(() -> {
                        AxPlayerWarps.getDatabase().removeFromList(warp, al, accessPlayer.player());
                        MESSAGEUTILS.sendLang(player, al.name().toLowerCase() + ".remove", Map.of("%player%", accessPlayer.name()));
                        open();
                    });
                }));
            }

            Scheduler.get().run(scheduledTask -> future.complete(null));
        });

        return future;
    }
}
