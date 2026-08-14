package com.artillexstudios.axplayerwarps.guis;

import com.artillexstudios.axapi.config.Config;
import com.artillexstudios.axapi.items.WrappedItemStack;
import com.artillexstudios.axapi.items.components.DataComponents;
import com.artillexstudios.axapi.libs.boostedyaml.settings.dumper.DumperSettings;
import com.artillexstudios.axapi.libs.boostedyaml.settings.general.GeneralSettings;
import com.artillexstudios.axapi.libs.boostedyaml.settings.loader.LoaderSettings;
import com.artillexstudios.axapi.libs.boostedyaml.settings.updater.UpdaterSettings;
import com.artillexstudios.axapi.scheduler.Scheduler;
import com.artillexstudios.axapi.utils.ItemBuilder;
import com.artillexstudios.axapi.utils.NumberUtils;
import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axguiframework.GuiFrame;
import com.artillexstudios.axguiframework.actions.GuiActions;
import com.artillexstudios.axguiframework.item.AxGuiItem;
import com.artillexstudios.axguiframework.libs.gui.guis.Gui;
import com.artillexstudios.axguiframework.replacements.Replacements;
import com.artillexstudios.axintegrations.types.CurrencyIntegration;
import com.artillexstudios.axplayerwarps.AxPlayerWarps;
import com.artillexstudios.axplayerwarps.category.Category;
import com.artillexstudios.axplayerwarps.category.CategoryManager;
import com.artillexstudios.axplayerwarps.enums.Access;
import com.artillexstudios.axplayerwarps.enums.AccessList;
import com.artillexstudios.axplayerwarps.input.InputManager;
import com.artillexstudios.axplayerwarps.user.Users;
import com.artillexstudios.axplayerwarps.user.WarpUser;
import com.artillexstudios.axplayerwarps.utils.WarpNameUtils;
import com.artillexstudios.axplayerwarps.warps.Warp;
import com.artillexstudios.axplayerwarps.warps.WarpManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.CONFIG;
import static com.artillexstudios.axplayerwarps.AxPlayerWarps.MESSAGEUTILS;

public class EditWarpGui extends GuiFrame<Gui> {
    private static final Config GUI = new Config(new File(AxPlayerWarps.getInstance().getDataFolder(), "guis/edit-warp.yml"),
            AxPlayerWarps.getInstance().getResource("guis/edit-warp.yml"),
            GeneralSettings.builder().setUseDefaults(false).build(),
            LoaderSettings.builder().build(),
            DumperSettings.DEFAULT,
            UpdaterSettings.builder().build()
    );

    private final Gui gui;
    private final Warp warp;
    private final WarpUser user;

    public EditWarpGui(Player player, Warp warp) {
        super(GUI.getInt("auto-update-ticks", -1), GUI, player);
        this.user = Users.get(player);
        this.warp = warp;

        gui = Gui.gui()
                .disableAllInteractions()
                .title(Component.empty())
                .rows(GUI.getInt("rows", 5))
                .create();

        addReplacement(new Replacements("%warp%", warp.getName()));
        addPlaceholderParameter(warp);

        setGui(gui, () -> parseText(GUI.getString("title", "")));
        user.addGui(this);

        gui.setPlayerInventoryAction(event -> {
            if (event.getCurrentItem() == null) return;
            warp.setIcon(event.getCurrentItem().getType());
            AxPlayerWarps.getThreadedQueue().submit(() -> {
                AxPlayerWarps.getDatabase().updateWarp(warp);
                MESSAGEUTILS.sendLang(player, "editor.update-icon");
            });
            open();
        });
    }

    public static boolean reload() {
        return GUI.reload();
    }

    public void open() {
        AxGuiItem guiItem = createItem("name-icon", event -> {
            GuiActions.run(player, this, event, section.getStringList("name-icon.actions"));
            if (event.isShiftClick() && event.isRightClick()) {
                warp.setIcon(null);
                AxPlayerWarps.getThreadedQueue().submit(() -> {
                    AxPlayerWarps.getDatabase().updateWarp(warp);
                    MESSAGEUTILS.sendLang(player, "editor.remove-icon");
                });
                open();
                return;
            }
            InputManager.getInput(player, "rename", result -> {
                if (result.isBlank()) {
                    MESSAGEUTILS.sendLang(player, "errors.invalid-name");
                    open();
                    return;
                }

                switch (WarpNameUtils.isAllowed(result)) {
                    case DISALLOWED -> {
                        MESSAGEUTILS.sendLang(player, "errors.disallowed-name-blacklisted");
                        return;
                    }
                    case CONTAINS_SPACES -> {
                        MESSAGEUTILS.sendLang(player, "errors.disallowed-name-space");
                        return;
                    }
                    case INVALID_LENGTH -> {
                        MESSAGEUTILS.sendLang(player, "errors.disallowed-name-length");
                        return;
                    }
                }

                AxPlayerWarps.getThreadedQueue().submit(() -> {
                    if (!warp.setName(result.replace(" ", "_"))) {
                        MESSAGEUTILS.sendLang(player, "errors.name-exists");
                    } else {
                            AxPlayerWarps.getDatabase().updateWarp(warp);
                            MESSAGEUTILS.sendLang(player, "editor.update-name");
                    }
                    Scheduler.get().run(() -> open());
                });
            });
        });

        ItemStack mt = guiItem.getItemStack();
        if (warp.getIcon() != null) mt.setType(warp.getIcon());
        guiItem.setItemStack(mt);

        createItem("location", event -> {
            GuiActions.run(player, this, event, section.getStringList("location.actions"));
            warp.setLocation(player.getLocation());
            AxPlayerWarps.getThreadedQueue().submit(() -> {
                AxPlayerWarps.getDatabase().updateWarp(warp);
                MESSAGEUTILS.sendLang(player, "editor.update-location");
            });
            open();
        });

        createItem("transfer", event -> {
            GuiActions.run(player, this, event, section.getStringList("transfer.actions"));
            InputManager.getInput(player, "transfer", result -> {
                player.closeInventory();
                if (result.equalsIgnoreCase(player.getName())) {
                    MESSAGEUTILS.sendLang(player, "errors.transfer-self");
                    return;
                }

                Player transferTo = Bukkit.getPlayer(result);
                if (transferTo == null) {
                    MESSAGEUTILS.sendLang(player, "errors.player-not-online", Map.of(
                            "%player%", result,
                            "%warp%", warp.getName()
                    ));
                    return;
                }
                WarpUser transferUser = Users.get(transferTo);
                long limit = transferUser.getWarpLimit();
                long warps = WarpManager.getWarps(transferTo).size();
                if (limit <= warps) {
                    MESSAGEUTILS.sendLang(player, "errors.transfer-failed", Map.of(
                            "%player%", transferTo.getName(),
                            "%warp%", warp.getName()
                    ));
                    return;
                }

                warp.setOwner(transferTo.getUniqueId());
                MESSAGEUTILS.sendLang(transferTo, "editor.new-owner", Map.of(
                        "%player%", player.getName(),
                        "%warp%", warp.getName()
                ));
                MESSAGEUTILS.sendLang(player, "editor.transferred", Map.of(
                        "%player%", transferTo.getName(),
                        "%warp%", warp.getName()
                ));

                AxPlayerWarps.getThreadedQueue().submit(() -> {
                    AxPlayerWarps.getDatabase().updateWarp(warp);
                    AxPlayerWarps.getDatabase().removeFromList(warp, AccessList.WHITELIST, transferTo);
                    AxPlayerWarps.getDatabase().removeFromList(warp, AccessList.BLACKLIST, transferTo);
                });
            });
        });

        createItem("access", event -> {
            GuiActions.run(player, this, event, section.getStringList("access.actions"));
            Access currAccess = warp.getAccess();
            ArrayList<Access> accesses = new ArrayList<>(List.of(Access.values()));
            int idx = accesses.indexOf(currAccess);
            if (event.isLeftClick()) {
                idx++;
                if (idx >= accesses.size()) idx = 0;
            } else if (event.isRightClick()) {
                if (event.isShiftClick()) {
                    idx = 0;
                } else {
                    idx--;
                    if (idx < 0) idx = accesses.size() - 1;
                }
            }
            warp.setAccess(accesses.get(idx));
            AxPlayerWarps.getThreadedQueue().submit(() -> AxPlayerWarps.getDatabase().updateWarp(warp));
            open();
        });

        createItem("category", event -> {
            GuiActions.run(player, this, event, section.getStringList("category.actions"));
            Category category = warp.getCategory();
            ArrayList<Category> categories = new ArrayList<>(CategoryManager.getCategories().values());
            int idx = category == null ? -1 : categories.indexOf(category);
            if (event.isLeftClick()) {
                idx++;
                if (idx >= categories.size()) idx = 0;
            } else if (event.isRightClick()) {
                if (idx == -1) idx = 0;
                if (event.isShiftClick()) {
                    idx = -1;
                } else {
                    idx--;
                    if (idx < 0) idx = categories.size() - 1;
                }
            }
            warp.setCategory(idx == -1 ? null : categories.get(idx));
            AxPlayerWarps.getThreadedQueue().submit(() -> AxPlayerWarps.getDatabase().updateWarp(warp));
            open();
        });

        createItem("price", event -> {
            GuiActions.run(player, this, event, section.getStringList("price.actions"));
            if (warp.getEarnedMoney() > 0) warp.withdrawMoney();
            CurrencyIntegration currency = warp.getCurrencyIntegration();
            List<CurrencyIntegration> currencies = CurrencyIntegration.list();
            int idx = currency == null ? -1 : currencies.indexOf(currency);
            if (event.isLeftClick()) {
                if (event.isShiftClick()) {
                    InputManager.getInput(player, "price", result -> {
                        if (!NumberUtils.isInt(result)) {
                            MESSAGEUTILS.sendLang(player, "errors.not-a-number");
                        } else {
                            int price = Integer.parseInt(result);
                            if (price < 1) {
                                MESSAGEUTILS.sendLang(player, "errors.must-be-positive");
                                open();
                                return;
                            }
                            warp.setTeleportPrice(price);
                            AxPlayerWarps.getThreadedQueue().submit(() -> AxPlayerWarps.getDatabase().updateWarp(warp));
                        }
                        open();
                    });
                    return;
                }
                idx++;
                if (idx >= currencies.size()) idx = 0;
            } else if (event.isRightClick()) {
                if (idx == -1) idx = 0;
                if (event.isShiftClick()) {
                    idx = -1;
                    warp.setTeleportPrice(0);
                } else {
                    idx--;
                    if (idx < 0) idx = currencies.size() - 1;
                }
            }
            warp.setCurrency(idx == -1 ? null : currencies.get(idx));
            AxPlayerWarps.getThreadedQueue().submit(() -> AxPlayerWarps.getDatabase().updateWarp(warp));
            open();
        });

        createItem("delete", event -> {
            if (event.isShiftClick() && event.isRightClick()) {
                GuiActions.run(player, this, event, section.getStringList("delete.actions"));
                warp.delete();
                Scheduler.get().runLaterAt(player.getLocation(), () -> {
                    player.closeInventory();
                }, 1);
            }
        });

        createItem("bank", event -> {
            GuiActions.run(player, this, event, section.getStringList("bank.actions"));
            warp.withdrawMoney();
            open();
        });

        ItemBuilder builder = ItemBuilder.create(section.getSection("description"));
        WrappedItemStack wrap = WrappedItemStack.wrap(builder.get());
        List<String> lore = new ArrayList<>();
        String[] description = warp.getDescription().split("\n", CONFIG.getInt("warp-description.max-lines", 3));
        for (Component line : wrap.get(DataComponents.LORE).lines()) {
            String serialized = StringUtils.MINI_MESSAGE.serialize(line);
            if (serialized.contains("%description%")) {
                for (String s : description) {
                    lore.add(serialized.replace("%description%", s));
                }
                continue;
            }
            lore.add(serialized);
        }
        builder.setLore(lore);

        createItem("description", builder.get(), event -> {
            GuiActions.run(player, this, event, section.getStringList("description.actions"));
            var realDesc = warp.getRealDescription();
            List<String> desc = realDesc == null ? new ArrayList<>() : new ArrayList<>(Arrays.stream(realDesc.split("\n")).toList());
            if (event.isLeftClick()) {
                if (CONFIG.getInt("warp-description.max-lines") <= desc.size()) {
                    MESSAGEUTILS.sendLang(player, "errors.max-lines");
                    open();
                    return;
                }
                InputManager.getInput(player, "add-line", result -> {
                    int maxLineLength = CONFIG.getInt("warp-description.max-line-length");
                    if (result.length() > CONFIG.getInt("warp-description.max-line-length")) {
                        MESSAGEUTILS.sendLang(player, "errors.max-length", Map.of("%length%", String.valueOf(maxLineLength)));
                        Scheduler.get().run(() -> open());
                        return;
                    }
                    desc.add(result);
                    warp.setDescription(desc);
                    AxPlayerWarps.getThreadedQueue().submit(() -> {
                        AxPlayerWarps.getDatabase().updateWarp(warp);
                        Scheduler.get().run(() -> open());
                    });
                });
                return;
            } else if (event.isRightClick()) {
                if (desc.isEmpty()) return;
                if (event.isShiftClick()) {
                    desc.clear();
                    warp.setDescription(desc);
                    AxPlayerWarps.getThreadedQueue().submit(() -> AxPlayerWarps.getDatabase().updateWarp(warp));
                    open();
                    return;
                }
                desc.removeLast();
                warp.setDescription(desc);
                AxPlayerWarps.getThreadedQueue().submit(() -> AxPlayerWarps.getDatabase().updateWarp(warp));
                open();
            }
        }, new Replacements(), List.of());

        createItem("whitelist", event -> {
            GuiActions.run(player, this, event, section.getStringList("whitelist.actions"));
            new WhitelistGui(player, warp).open();
        });

        createItem("blacklist", event -> {
            GuiActions.run(player, this, event, section.getStringList("blacklist.actions"));
            new BlacklistGui(player, warp).open();
        });

        gui.update();
        gui.open(player);
    }
}
