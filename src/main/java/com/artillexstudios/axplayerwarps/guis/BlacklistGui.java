package com.artillexstudios.axplayerwarps.guis;

import com.artillexstudios.axapi.config.Config;
import com.artillexstudios.axapi.libs.boostedyaml.settings.dumper.DumperSettings;
import com.artillexstudios.axapi.libs.boostedyaml.settings.general.GeneralSettings;
import com.artillexstudios.axapi.libs.boostedyaml.settings.loader.LoaderSettings;
import com.artillexstudios.axapi.libs.boostedyaml.settings.updater.UpdaterSettings;
import com.artillexstudios.axapi.nms.wrapper.ServerPlayerWrapper;
import com.artillexstudios.axapi.placeholders.PlaceholderHandler;
import com.artillexstudios.axapi.scheduler.Scheduler;
import com.artillexstudios.axapi.utils.ItemBuilder;
import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axguiframework.GuiFrame;
import com.artillexstudios.axguiframework.actions.GuiActions;
import com.artillexstudios.axguiframework.item.AxGuiItem;
import com.artillexstudios.axplayerwarps.AxPlayerWarps;
import com.artillexstudios.axplayerwarps.database.impl.Base;
import com.artillexstudios.axplayerwarps.enums.AccessList;
import com.artillexstudios.axplayerwarps.input.InputManager;
import com.artillexstudios.axplayerwarps.user.Users;
import com.artillexstudios.axplayerwarps.user.WarpUser;
import com.artillexstudios.axplayerwarps.warps.Warp;
import com.artillexstudios.gui.guis.Gui;
import com.artillexstudios.gui.guis.PaginatedGui;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.MESSAGEUTILS;

public class BlacklistGui extends GuiFrame {
    private static final Config GUI = new Config(new File(AxPlayerWarps.getInstance().getDataFolder(), "guis/blacklist.yml"),
            AxPlayerWarps.getInstance().getResource("guis/blacklist.yml"),
            GeneralSettings.builder().setUseDefaults(false).build(),
            LoaderSettings.builder().build(),
            DumperSettings.DEFAULT,
            UpdaterSettings.builder().build()
    );

    private final PaginatedGui gui;
    private final Warp warp;
    private final WarpUser user;
    private static final AccessList al = AccessList.BLACKLIST;

    public BlacklistGui(Player player, Warp warp) {
        super(GUI.getInt("auto-update-ticks", -1), GUI, player);
        this.user = Users.get(player);
        this.warp = warp;
        this.gui = Gui.paginated()
            .disableAllInteractions()
            .title(StringUtils.format(GUI.getString("title", ""), Map.of("%warp%", warp.getName())))
            .rows(GUI.getInt("rows", 5))
            .pageSize(GUI.getInt("page-size", 21))
            .create();

        addPlaceholderParameter(warp);
        setGui(gui);
        user.addGui(this);
    }

    public static boolean reload() {
        return GUI.reload();
    }

    public void open() {
        createItem("add", event -> {
            GuiActions.run(player, this, event, file.getStringList("add.actions"));
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
                    Scheduler.get().run(() -> open());
                });
            });
        });

        load().thenRun(() -> {
            updateTitle();
            Scheduler.get().runAt(player.getLocation(), task -> gui.open(player));
        });
    }

    public void update() {
        load().thenRun(gui::update);
    }

    public CompletableFuture<Void> load() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        AxPlayerWarps.getThreadedQueue().submit(() -> {
            gui.clearPageItems();
            for (Base.AccessPlayer accessPlayer : warp.getAccessList(al)) {
                ItemBuilder builder = ItemBuilder.create(file.getSection(al.getRoute()));
                if (builder.get().getType() == Material.PLAYER_HEAD) {
                    Player pl = Bukkit.getPlayer(warp.getOwner());
                    if (pl != null) {
                        ServerPlayerWrapper wrapper = ServerPlayerWrapper.wrap(pl);
                        var textures = wrapper.textures();
                        if (textures.texture() != null) builder.setTextureValue(textures.texture());
                    }
                }

                builder.setName(PlaceholderHandler.parse(GUI.getString(al.getRoute() + ".name"), accessPlayer, player));
                List<String> lore = new ArrayList<>(GUI.getStringList(al.getRoute() + ".lore"));
                lore.replaceAll(s -> {
                    return PlaceholderHandler.parse(s, accessPlayer, player);
                });
                builder.setLore(lore);

                gui.addItem(new AxGuiItem(builder.get(), event -> {
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
