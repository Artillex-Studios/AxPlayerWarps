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
import com.artillexstudios.axguiframework.PaginatedGuiFrame;
import com.artillexstudios.axguiframework.actions.GuiActions;
import com.artillexstudios.axguiframework.item.AxGuiItem;
import com.artillexstudios.axguiframework.libs.gui.guis.Gui;
import com.artillexstudios.axguiframework.libs.gui.guis.PaginatedGui;
import com.artillexstudios.axguiframework.replacements.Replacements;
import com.artillexstudios.axplayerwarps.AxPlayerWarps;
import com.artillexstudios.axplayerwarps.database.impl.Base;
import com.artillexstudios.axplayerwarps.enums.AccessList;
import com.artillexstudios.axplayerwarps.input.InputManager;
import com.artillexstudios.axplayerwarps.user.Users;
import com.artillexstudios.axplayerwarps.user.WarpUser;
import com.artillexstudios.axplayerwarps.warps.Warp;
import net.kyori.adventure.text.Component;
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

public class BlacklistGui extends PaginatedGuiFrame {
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
    private static final AccessList accessList = AccessList.BLACKLIST;

    public BlacklistGui(Player player, Warp warp) {
        super(GUI.getInt("auto-update-ticks", -1), GUI, player);
        this.user = Users.get(player);
        this.warp = warp;

        gui = Gui.paginated()
                .disableAllInteractions()
                .title(Component.empty())
                .rows(GUI.getInt("rows", 5))
                .pageSize(GUI.getInt("page-size", 27))
                .create();

        addReplacement(new Replacements("%page%", () -> String.valueOf(gui.getCurrentPageNum())));
        addReplacement(new Replacements("%current_page%", () -> String.valueOf(gui.getCurrentPageNum())));
        addReplacement(new Replacements("%max_page%", () -> String.valueOf(gui.getPagesNum())));
        addReplacement(new Replacements("%pages%", () -> String.valueOf(gui.getPagesNum())));
        addReplacement(new Replacements("%warp%", warp.getName()));
        addPlaceholderParameter(warp);

        setGui(gui, () -> parseText(GUI.getString("title", "")));
        user.addGui(this);
    }

    public static boolean reload() {
        return GUI.reload();
    }

    public void open() {
        createItem("add", event -> {
            GuiActions.run(player, this, event, section.getStringList("add.actions"));
            if (event.isRightClick() && event.isShiftClick()) {
                AxPlayerWarps.getThreadedQueue().submit(() -> {
                    AxPlayerWarps.getDatabase().clearList(warp, accessList);
                    MESSAGEUTILS.sendLang(player, accessList.name().toLowerCase() + ".clear");
                    open();
                });
                return;
            }
            InputManager.getInput(player, "add-player", result -> {
                if (result.equalsIgnoreCase(player.getName())) {
                    MESSAGEUTILS.sendLang(player, "errors." + accessList.name().toLowerCase() + "-self");
                    open();
                    return;
                }
                AxPlayerWarps.getThreadedQueue().submit(() -> {
                    UUID uuid = AxPlayerWarps.getDatabase().getUUIDFromName(result);
                    if (uuid == null) {
                        MESSAGEUTILS.sendLang(player, "errors.player-not-found");
                    } else {
                        AxPlayerWarps.getDatabase().addToList(warp, accessList, Bukkit.getOfflinePlayer(uuid));
                        MESSAGEUTILS.sendLang(player, accessList.name().toLowerCase() + ".add", Map.of("%player%", result));
                    }
                    Scheduler.get().run(() -> open());
                });
            });
        });

        load().thenRun(() -> {
            updateTitle();
            gui.open(player);
        });
    }

    public void update() {
        load().thenRun(() -> {
            gui.update();
        });
    }

    public CompletableFuture<Void> load() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        AxPlayerWarps.getThreadedQueue().submit(() -> {
            gui.clearPageItems();
            for (Base.AccessPlayer accessPlayer : warp.getAccessList(accessList)) {
                ItemBuilder builder = ItemBuilder.create(section.getSection(accessList.getRoute()));
                if (builder.get().getType() == Material.PLAYER_HEAD) {
                    Player pl = Bukkit.getPlayer(warp.getOwner());
                    if (pl != null) {
                        ServerPlayerWrapper wrapper = ServerPlayerWrapper.wrap(pl);
                        var textures = wrapper.textures();
                        if (textures.texture() != null) builder.setTextureValue(textures.texture());
                    }
                }

                builder.setName(PlaceholderHandler.parse(GUI.getString(accessList.getRoute() + ".name"), accessPlayer, player));
                List<String> lore = new ArrayList<>(GUI.getStringList(accessList.getRoute() + ".lore"));
                lore.replaceAll(s -> {
                    return PlaceholderHandler.parse(s, accessPlayer, player);
                });
                builder.setLore(lore);

                gui.addItem(new AxGuiItem(builder.get(), event -> {
                    AxPlayerWarps.getThreadedQueue().submit(() -> {
                        AxPlayerWarps.getDatabase().removeFromList(warp, accessList, accessPlayer.player());
                        MESSAGEUTILS.sendLang(player, accessList.name().toLowerCase() + ".remove", Map.of("%player%", accessPlayer.name()));
                        open();
                    });
                }));
            }

            Scheduler.get().run(scheduledTask -> {
                future.complete(null);
            });
        });

        return future;
    }
}
