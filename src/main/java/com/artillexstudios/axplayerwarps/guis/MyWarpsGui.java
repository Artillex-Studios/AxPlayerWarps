package com.artillexstudios.axplayerwarps.guis;

import com.artillexstudios.axapi.config.Config;
import com.artillexstudios.axapi.libs.boostedyaml.settings.dumper.DumperSettings;
import com.artillexstudios.axapi.libs.boostedyaml.settings.general.GeneralSettings;
import com.artillexstudios.axapi.libs.boostedyaml.settings.loader.LoaderSettings;
import com.artillexstudios.axapi.libs.boostedyaml.settings.updater.UpdaterSettings;
import com.artillexstudios.axapi.nms.wrapper.ServerPlayerWrapper;
import com.artillexstudios.axapi.scheduler.Scheduler;
import com.artillexstudios.axapi.utils.AsyncUtils;
import com.artillexstudios.axapi.utils.ItemBuilder;
import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axguiframework.GuiFrame;
import com.artillexstudios.axguiframework.actions.GuiActions;
import com.artillexstudios.axguiframework.item.AxGuiItem;
import com.artillexstudios.axguiframework.replacements.Replacements;
import com.artillexstudios.axplayerwarps.AxPlayerWarps;
import com.artillexstudios.axplayerwarps.category.Category;
import com.artillexstudios.axplayerwarps.input.InputManager;
import com.artillexstudios.axplayerwarps.sorting.WarpComparator;
import com.artillexstudios.axplayerwarps.user.Users;
import com.artillexstudios.axplayerwarps.user.WarpUser;
import com.artillexstudios.axplayerwarps.warps.Warp;
import com.artillexstudios.axplayerwarps.warps.WarpManager;
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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.CONFIG;
import static com.artillexstudios.axplayerwarps.AxPlayerWarps.LANG;
import static com.artillexstudios.axplayerwarps.AxPlayerWarps.MESSAGEUTILS;

public class MyWarpsGui extends GuiFrame {
    private static final Config GUI = new Config(new File(AxPlayerWarps.getInstance().getDataFolder(), "guis/my-warps.yml"),
            AxPlayerWarps.getInstance().getResource("guis/my-warps.yml"),
            GeneralSettings.builder().setUseDefaults(false).build(),
            LoaderSettings.builder().build(),
            DumperSettings.DEFAULT,
            UpdaterSettings.builder().build()
    );

    private final PaginatedGui gui = Gui
            .paginated()
            .disableAllInteractions()
            .title(Component.empty())
            .rows(GUI.getInt("rows", 5))
            .pageSize(GUI.getInt("page-size", 27))
            .create();

    private Category category = null;
    private String search = null;
    private final WarpUser user;

    public MyWarpsGui(Player player, Category category, String search) {
        this(player, category);
        this.search = search;
    }

    public MyWarpsGui(Player player, Category category) {
        this(player);
        this.category = category;
    }

    public MyWarpsGui(Player player) {
        super(GUI.getInt("auto-update-ticks", -1), GUI, player);
        this.user = Users.get(player);

        addReplacement(new Replacements("%search%", () -> search == null ? LANG.getString("placeholders.no-search") : search));
        addReplacement(new Replacements("%category_selected%", () -> category == null ? LANG.getString("placeholders.no-category") : category.formatted()));
        setGui(gui);
        user.addGui(this);
    }

    public static boolean reload() {
        return GUI.reload();
    }

    public void open() {
        open(1);
    }

    public void open(int page) {
        createItem("search", event -> {
            GuiActions.run(player, this, event, file.getStringList("search.actions"));
            if (event.isShiftClick()) {
                search = null;
                MESSAGEUTILS.sendLang(player, "search.reset");
                open();
                return;
            }
            InputManager.getInput(player, "search", result -> {
                if (result.isBlank()) search = null;
                else search = result;
                if (search == null)
                    MESSAGEUTILS.sendLang(player, "search.reset");
                else
                    MESSAGEUTILS.sendLang(player, "search.show", Map.of("%search%", search));
                open();
            });
        });

        createItem("sorting", event -> {
            GuiActions.run(player, this, event, file.getStringList("sorting.actions"));
            if (event.isShiftClick()) {
                user.resetSorting();
            } else {
                if (event.isLeftClick()) user.changeSorting(1);
                if (event.isRightClick()) user.changeSorting(-1);
            }
            open(gui.getCurrentPageNum());
        });

        createItem("category", event -> {
            GuiActions.run(player, this, event, file.getStringList("category.actions"));
            if (event.isShiftClick()) {
                user.resetCategory();
                category = null;
                open(gui.getCurrentPageNum());
                return;
            }
            if (event.isLeftClick()) user.changeCategory(1);
            if (event.isRightClick()) user.changeCategory(-1);
            category = user.getCategory();
            open(gui.getCurrentPageNum());
        });

        loadWarps().thenRun(() -> {
            if (player != null) {
                Scheduler.get().runAt(player.getLocation(), task -> gui.open(player, page));
            }
        });
    }

    public void update() {
        loadWarps().thenRun(gui::update);
    }

    @Override
    public void updateTitle() {
        gui.updateTitle(StringUtils.format(GUI.getString("title", ""), new HashMap<>(Map.of("%page%", "" + gui.getCurrentPageNum(), "%pages%", "" + Math.max(1, gui.getPagesNum())))));
    }

    public CompletableFuture<Void> loadWarps() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        AsyncUtils.submit(() -> {
            var filtered = WarpManager.getWarps()
                    .stream()
                    .filter(warp -> warp.getOwner().equals(player.getUniqueId()))
                    .sorted(new WarpComparator(user.getSorting(), player))
                    .toList();

            AxGuiItem[] axGuiItems = new AxGuiItem[filtered.size()];
            List<CompletableFuture<?>> futures = new ArrayList<>();
            int i = 0;
            for (Warp warp : filtered) {

                // category
                if (category != null && !Objects.equals(warp.getCategory(), category)) continue;

                // search
                if (search != null && (!warp.getName().toLowerCase().contains(search) && !warp.getOwnerName().toLowerCase().contains(search))) continue;

                CompletableFuture<Void> completableFuture = new CompletableFuture<>();
                futures.add(completableFuture);

                int i2 = i;
                AsyncUtils.submit(() -> {
                    Material icon = warp.getIcon();
                    ItemBuilder builder = ItemBuilder.create(new ItemStack(icon));
                    builder.setName(parseText(GUI.getString("warp.name"), warp));

                    String[] description = warp.getDescription().split("\n", CONFIG.getInt("warp-description.max-lines", 3));

                    List<String> lore = new ArrayList<>();
                    List<String> lore2 = new ArrayList<>(GUI.getStringList("warp.lore"));
                    for (int j = 0; j < lore2.size(); j++) {
                        String line = lore2.get(j);
                        if (!line.contains("%description%")) {
                            lore.add(line);
                            continue;
                        }
                        for (int k = description.length - 1; k >= 0; k--) {
                            lore.add(j, line.replace("%description%", description[k]));
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

                    AxGuiItem axGuiItem = new AxGuiItem(builder.get(), event -> {
                        if (event.isLeftClick()) {
                            warp.teleportPlayer(player);
                        } else {
                            if (!player.hasPermission("axplayerwarps.edit")) {
                                MESSAGEUTILS.sendLang(player, "errors.no-permission");
                                return;
                            }
                            new EditWarpGui(player, warp).open();
                        }
                    });

                    axGuiItems[i2] = axGuiItem;
                    completableFuture.complete(null);
                });
                i++;
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
                gui.clearPageItems();
                for (AxGuiItem axGuiItem : axGuiItems) {
                    if (axGuiItem == null) continue;
                    gui.addItem(axGuiItem);
                }

                Scheduler.get().run(scheduledTask -> {
                    updateTitle();
                    future.complete(null);
                });
            });
        });

        return future;
    }
}
