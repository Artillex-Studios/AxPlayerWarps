package com.artillexstudios.axplayerwarps.guis;

import com.artillexstudios.axapi.config.Config;
import com.artillexstudios.axapi.libs.boostedyaml.settings.dumper.DumperSettings;
import com.artillexstudios.axapi.libs.boostedyaml.settings.general.GeneralSettings;
import com.artillexstudios.axapi.libs.boostedyaml.settings.loader.LoaderSettings;
import com.artillexstudios.axapi.libs.boostedyaml.settings.updater.UpdaterSettings;
import com.artillexstudios.axapi.scheduler.Scheduler;
import com.artillexstudios.axapi.utils.NumberUtils;
import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axguiframework.GuiFrame;
import com.artillexstudios.axguiframework.actions.GuiActions;
import com.artillexstudios.axplayerwarps.AxPlayerWarps;
import com.artillexstudios.axplayerwarps.input.InputManager;
import com.artillexstudios.axplayerwarps.user.Users;
import com.artillexstudios.axplayerwarps.user.WarpUser;
import com.artillexstudios.axplayerwarps.warps.Warp;
import com.artillexstudios.gui.guis.Gui;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;
import java.util.Map;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.MESSAGEUTILS;

public class RateWarpGui extends GuiFrame {
    private static final Config GUI = new Config(new File(AxPlayerWarps.getInstance().getDataFolder(), "guis/rate-warp.yml"),
            AxPlayerWarps.getInstance().getResource("guis/rate-warp.yml"),
            GeneralSettings.builder().setUseDefaults(false).build(),
            LoaderSettings.builder().build(),
            DumperSettings.DEFAULT,
            UpdaterSettings.builder().build()
    );

    private final Gui gui;
    private final Warp warp;
    private final WarpUser user;

    public RateWarpGui(Player player, Warp warp) {
        super(GUI.getInt("auto-update-ticks", -1), GUI, player);
        this.user = Users.get(player);
        this.warp = warp;
        this.gui = Gui.gui()
            .disableAllInteractions()
            .title(StringUtils.format(GUI.getString("title", ""), Map.of("%warp%", warp.getName())))
            .rows(GUI.getInt("rows", 5))
            .create();

        addPlaceholderParameter(warp);
        setGui(gui);
        user.addGui(this);
    }

    public static boolean reload() {
        return GUI.reload();
    }

    public void open() {
        final List<String> slots = file.getBackingDocument().getStringList("favorite.slot");
        var slotOverrides = getSlots(slots.isEmpty() ? List.of(file.getString("favorite.slot")) : slots);

        boolean isFavorite = user.getFavorites().contains(warp);
        createItem("favorite." + (isFavorite ? "favorite" : "not-favorite"), event -> {
            GuiActions.run(player, this, event, file.getStringList("favorite.actions"));
            AxPlayerWarps.getThreadedQueue().submit(() -> {
                if (isFavorite) {
                    AxPlayerWarps.getDatabase().removeFromFavorites(player, warp);
                    MESSAGEUTILS.sendLang(player, "favorite.remove", Map.of("%warp%", warp.getName()));
                } else {
                    AxPlayerWarps.getDatabase().addToFavorites(player, warp);
                    MESSAGEUTILS.sendLang(player, "favorite.add", Map.of("%warp%", warp.getName()));
                }
                Scheduler.get().run(() -> open());
            });
        }, slotOverrides);

        createItem("teleport", event -> {
            GuiActions.run(player, this, event, file.getStringList("teleport.actions"));
            warp.teleportPlayer(player);
        });

        createItem("rate", event -> {
            GuiActions.run(player, this, event, file.getStringList("rate.actions"));
            if (event.isRightClick()) {
                AxPlayerWarps.getThreadedQueue().submit(() -> {
                    AxPlayerWarps.getDatabase().removeRating(player, warp);
                    MESSAGEUTILS.sendLang(player, "rate.remove");
                    Scheduler.get().run(() -> open());
                });
            }
            if (event.isLeftClick()) {
                InputManager.getInput(player, "rate", result -> {
                    if (!NumberUtils.isInt(result)) {
                        MESSAGEUTILS.sendLang(player, "errors.not-a-number");
                    } else {
                        int i = Math.max(1, Math.min(5, Integer.parseInt(result)));
                        AxPlayerWarps.getThreadedQueue().submit(() -> {
                            AxPlayerWarps.getDatabase().setRating(player, warp, i);
                            MESSAGEUTILS.sendLang(player, "rate.add", Map.of("%rating%", "" + i));
                        });
                    }
                    Scheduler.get().run(() -> open());
                });
            }
        });

        gui.update();
        Scheduler.get().runAt(player.getLocation(), task -> gui.open(player));
    }
}
