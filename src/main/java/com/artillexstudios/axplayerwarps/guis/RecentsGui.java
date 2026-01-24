package com.artillexstudios.axplayerwarps.guis;

import com.artillexstudios.axapi.config.Config;
import com.artillexstudios.axapi.libs.boostedyaml.settings.dumper.DumperSettings;
import com.artillexstudios.axapi.libs.boostedyaml.settings.general.GeneralSettings;
import com.artillexstudios.axapi.libs.boostedyaml.settings.loader.LoaderSettings;
import com.artillexstudios.axapi.libs.boostedyaml.settings.updater.UpdaterSettings;
import com.artillexstudios.axapi.nms.wrapper.ServerPlayerWrapper;
import com.artillexstudios.axapi.scheduler.Scheduler;
import com.artillexstudios.axapi.utils.ItemBuilder;
import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axguiframework.GuiFrame;
import com.artillexstudios.axguiframework.item.AxGuiItem;
import com.artillexstudios.axplayerwarps.AxPlayerWarps;
import com.artillexstudios.axplayerwarps.user.Users;
import com.artillexstudios.axplayerwarps.user.WarpUser;
import com.artillexstudios.axplayerwarps.warps.Warp;
import com.artillexstudios.gui.guis.Gui;
import com.artillexstudios.gui.guis.PaginatedGui;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.CONFIG;

public class RecentsGui extends GuiFrame {
        private static final Config GUI = new Config(new File(AxPlayerWarps.getInstance().getDataFolder(), "guis/recents.yml"),
            AxPlayerWarps.getInstance().getResource("guis/recents.yml"),
            GeneralSettings.builder().setUseDefaults(false).build(),
            LoaderSettings.builder().build(),
            DumperSettings.DEFAULT,
            UpdaterSettings.builder().build()
    );

    private final PaginatedGui gui;
    private final WarpUser user;

    public RecentsGui(Player player) {
        super(GUI.getInt("auto-update-ticks", -1), GUI, player);
        this.user = Users.get(player);
        this.gui = Gui.paginated()
            .disableAllInteractions()
            .title(Component.empty())
            .rows(GUI.getInt("rows", 5))
            .pageSize(GUI.getInt("page-size", 21))
            .create();

        setGui(gui);
        user.addGui(this);
    }

    @Override
    public void updateTitle() {
        gui.updateTitle(StringUtils.format(GUI.getString("title", ""), new HashMap<>(Map.of("%page%", "" + gui.getCurrentPageNum(), "%pages%", "" + Math.max(1, gui.getPagesNum())))));
    }

    public static boolean reload() {
        return GUI.reload();
    }

    public void open() {
        load().thenRun(() -> {
            updateTitle();
            Scheduler.get().runAt(player.getLocation(), task -> gui.open(player));
        });
    }

    public void update() {
        load().thenRun(gui::update);
    }

    public CompletableFuture<Void> load() {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        AxPlayerWarps.getThreadedQueue().submit(() -> {
            gui.clearPageItems();
            for (Warp warp : AxPlayerWarps.getDatabase().getRecentWarps(player)) {
                Material icon = warp.getIcon();
                ItemBuilder builder = ItemBuilder.create(new ItemStack(icon));
                builder.setName(parseText(GUI.getString("warp.name"), warp));

                String[] description = warp.getDescription().split("\n", CONFIG.getInt("warp-description.max-lines", 3));

                List<String> lore = new ArrayList<>();
                List<String> lore2 = new ArrayList<>(GUI.getStringList("warp.lore"));
                for (int i = 0; i < lore2.size(); i++) {
                    String line = lore2.get(i);
                    if (!line.contains("%description%")) {
                        lore.add(line);
                        continue;
                    }
                    for (int j = description.length - 1; j >= 0; j--) {
                        lore.add(i, line.replace("%description%", description[j]));
                    }
                }
                builder.setLore(parseText(lore, warp));
                if (icon == Material.PLAYER_HEAD) {
                    Player pl = Bukkit.getPlayer(warp.getOwner());
                    if (pl != null) {
                        ServerPlayerWrapper wrapper = ServerPlayerWrapper.wrap(pl);
                        var textures = wrapper.textures();
                        if (textures.texture() != null) builder.setTextureValue(textures.texture());
                    }
                }
                gui.addItem(new AxGuiItem(builder.get(), event -> {
                    if (event.isLeftClick()) {
                        warp.teleportPlayer(player);
                    } else {
                        new RateWarpGui(player, warp).open();
                    }
                }));
            }

            Scheduler.get().run(scheduledTask -> {
                future.complete(null);
            });
        });

        return future;
    }
}
