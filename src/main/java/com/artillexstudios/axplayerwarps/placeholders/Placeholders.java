package com.artillexstudios.axplayerwarps.placeholders;

import com.artillexstudios.axapi.items.WrappedItemStack;
import com.artillexstudios.axapi.items.component.DataComponents;
import com.artillexstudios.axapi.items.component.ItemLore;
import com.artillexstudios.axapi.utils.Pair;
import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axplayerwarps.AxPlayerWarps;
import com.artillexstudios.axplayerwarps.utils.StarUtils;
import com.artillexstudios.axplayerwarps.utils.TimeUtils;
import com.artillexstudios.axplayerwarps.warps.Warp;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.CONFIG;
import static com.artillexstudios.axplayerwarps.AxPlayerWarps.LANG;

public class Placeholders {
    public static final DecimalFormat df = new DecimalFormat("#.##");

    // todo: optimize
    public static String parse(Warp warp, @Nullable OfflinePlayer player, String t) {
        t = t.replace("%name%", warp.getName());
        t = t.replace("%owner%", AxPlayerWarps.getDatabase().getPlayerName(warp.getOwner()));
        t = t.replace("%created%", TimeUtils.formatDate(warp.getCreated()));
        t = t.replace("%world%", warp.getLocation().getWorld().getName());
        t = t.replace("%x%", df.format(warp.getLocation().getX()));
        t = t.replace("%y%", df.format(warp.getLocation().getY()));
        t = t.replace("%z%", df.format(warp.getLocation().getZ()));
        t = t.replace("%yaw%", df.format(warp.getLocation().getYaw()));
        t = t.replace("%pitch%", df.format(warp.getLocation().getPitch()));
        if (warp.getCategory() != null)
            t = t.replace("%category%", CONFIG.getString("categories." + warp.getCategory().raw() + ".name"));
        else
            t = t.replace("%category%", LANG.getString("placeholders.no-category"));
        double price = warp.getCurrency() == null ? 0 : warp.getTeleportPrice();
        t = t.replace("%price%", price == 0 ? LANG.getString("placeholders.free") : df.format(price));
        t = t.replace("%access%", LANG.getString("access." + warp.getAccess().name().toLowerCase()));

        Pair<Integer, Float> rating = AxPlayerWarps.getDatabase().getRatings(warp);
        t = t.replace("%rating_decimal%", df.format(rating.getValue()));
        int starAm = Math.round(rating.getValue());
        t = t.replace("%rating_stars%", StarUtils.getFormatted(starAm, 5));
        t = t.replace("%rating_amount%", "" + rating.getKey());

        return t;
    }

    public static List<String> parseList(Warp warp, @Nullable OfflinePlayer player, List<String> t) {
        t.replaceAll(s -> parse(warp, player, s)); return t;
    }

    public static ItemStack parseItem(Warp warp, @Nullable OfflinePlayer player, ItemStack it) {
        WrappedItemStack wrap = WrappedItemStack.wrap(it);
        Component nameComponent = wrap.get(DataComponents.customName());
        ItemLore itemLore = wrap.get(DataComponents.lore());
        wrap.set(DataComponents.customName(), parse(warp, player, nameComponent));
        wrap.set(DataComponents.lore(), new ItemLore(parseListComponent(warp, player, itemLore.lines())));
        return wrap.toBukkit();
    }

    private static final MiniMessage mm = MiniMessage.builder().build();
    public static Component parse(Warp warp, @Nullable OfflinePlayer player, Component t) {
        String string = mm.serialize(t);
        string = parse(warp, player, string);
        return StringUtils.format(string);
    }

    public static List<Component> parseListComponent(Warp warp, @Nullable OfflinePlayer player, List<Component> t) {
        List<String> strings = new ArrayList<>();
        for (Component s : t) {
            String string = mm.serialize(s);
            string = parse(warp, player, string);
            strings.add(string);
        }
        return StringUtils.formatList(strings);
    }
}
