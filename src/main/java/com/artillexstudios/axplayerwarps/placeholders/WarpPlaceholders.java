package com.artillexstudios.axplayerwarps.placeholders;

import com.artillexstudios.axapi.placeholders.PlaceholderArgument;
import com.artillexstudios.axapi.placeholders.PlaceholderArguments;
import com.artillexstudios.axapi.placeholders.PlaceholderContext;
import com.artillexstudios.axapi.placeholders.PlaceholderHandler;
import com.artillexstudios.axapi.placeholders.exception.PlaceholderException;
import com.artillexstudios.axapi.utils.functions.ThrowingFunction;
import com.artillexstudios.axintegrations.types.CurrencyIntegration;
import com.artillexstudios.axplayerwarps.AxPlayerWarps;
import com.artillexstudios.axplayerwarps.database.impl.Base;
import com.artillexstudios.axplayerwarps.enums.AccessList;
import com.artillexstudios.axplayerwarps.hooks.HookManager;
import com.artillexstudios.axplayerwarps.placeholders.resolvers.WarpResolver;
import com.artillexstudios.axplayerwarps.user.Users;
import com.artillexstudios.axplayerwarps.user.WarpUser;
import com.artillexstudios.axplayerwarps.utils.FormatUtils;
import com.artillexstudios.axplayerwarps.utils.StarUtils;
import com.artillexstudios.axplayerwarps.utils.TimeUtils;
import com.artillexstudios.axplayerwarps.warps.Warp;
import com.artillexstudios.axplayerwarps.warps.WarpManager;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.Optional;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.CONFIG;
import static com.artillexstudios.axplayerwarps.AxPlayerWarps.LANG;

public class WarpPlaceholders {
    private static final PlaceholderArgument<Warp> warpArg = new PlaceholderArgument<>("warp", WarpResolver.class);
    public static final DecimalFormat df = new DecimalFormat("#.##");
    private static String noCategory;

    public static String format(Object obj) {
        return df.format(obj);
    }

    public static void reload() {
        noCategory = LANG.getString("placeholders.no-category");
    }

    public static void load() {
        reload();

        String empty = "";

        // global
        ThrowingFunction<PlaceholderContext, String, PlaceholderException> totalHandler = handler -> {
            return String.valueOf(WarpManager.getWarps().size());
        };
        PlaceholderHandler.register("total_warps", totalHandler, true);
        PlaceholderHandler.register("all_warps", totalHandler, true);

        // player
        PlaceholderHandler.register("player_warp_limit", handler -> {
            Player player = handler.resolve(Player.class);
            if (player == null) return empty;
            WarpUser user = Users.get(player);
            return String.valueOf(user.getWarpLimit());
        }, true);

        ThrowingFunction<PlaceholderContext, String, PlaceholderException> playerWarpsHandler =handler -> {
            Player player = handler.resolve(Player.class);
            if (player == null) return empty;
            return String.valueOf(WarpManager.getWarps().stream().filter(warp -> warp.getOwner().equals(player.getUniqueId())).count());
        };
        PlaceholderHandler.register("player_warps", playerWarpsHandler, true);
        PlaceholderHandler.register("my_warps", playerWarpsHandler, true);

        PlaceholderHandler.register("favorite_warps", handler -> {
            Player player = handler.resolve(Player.class);
            if (player == null) return empty;
            WarpUser user = Users.get(player);
            return String.valueOf(user.getFavorites().size());
        }, true);

        PlaceholderHandler.register("sorting_selected", handler -> {
            Player player = handler.resolve(Player.class);
            if (player == null) return empty;
            WarpUser user = Users.get(player);
            return user.getSorting().name();
        }, true);


        // access player
        PlaceholderHandler.register("player", handler -> {
            Base.AccessPlayer accessPlayer = handler.raw(Base.AccessPlayer.class);
            if (accessPlayer == null) return empty;
            return accessPlayer.name();
        }, false);

        PlaceholderHandler.register("added-date", handler -> {
            Base.AccessPlayer accessPlayer = handler.raw(Base.AccessPlayer.class);
            if (accessPlayer == null) return empty;
            return TimeUtils.formatDate(accessPlayer.added());
        }, false);

        // warp
        registerWarp("id", handler -> {
            Warp warp = getWarp(handler);
            if (warp == null) return empty;
            return String.valueOf(warp.getId());
        });

        registerWarp("name", handler -> {
            Warp warp = getWarp(handler);
            if (warp == null) return empty;
            return warp.getName();
        });

        registerWarp("owner", handler -> {
            Warp warp = getWarp(handler);
            if (warp == null) return empty;
            return AxPlayerWarps.getDatabase().getPlayerName(warp.getOwner());
        });

        registerWarp("created", handler -> {
            Warp warp = getWarp(handler);
            if (warp == null) return empty;
            return TimeUtils.formatDate(warp.getCreated());
        });

        registerWarp("world", handler -> {
            Warp warp = getWarp(handler);
            if (warp == null) return empty;
            World world = warp.getLocation().getWorld();
            if (world == null) return empty;
            return Optional.of(world.getName()).orElse("---");
        });

        registerWarp("x", handler -> {
            Warp warp = getWarp(handler);
            if (warp == null) return empty;
            return format(warp.getLocation().getX());
        });

        registerWarp("y", handler -> {
            Warp warp = getWarp(handler);
            if (warp == null) return empty;
            return format(warp.getLocation().getY());
        });

        registerWarp("z", handler -> {
            Warp warp = getWarp(handler);
            if (warp == null) return empty;
            return format(warp.getLocation().getZ());
        });

        registerWarp("yaw", handler -> {
            Warp warp = getWarp(handler);
            if (warp == null) return empty;
            return format(warp.getLocation().getYaw());
        });

        registerWarp("pitch", handler -> {
            Warp warp = getWarp(handler);
            if (warp == null) return empty;
            return format(warp.getLocation().getPitch());
        });

        registerWarp("category", handler -> {
            Warp warp = getWarp(handler);
            if (warp == null) return empty;
            if (warp.getCategory() == null) {
                return noCategory;
            } else {
                return CONFIG.getString("categories." + warp.getCategory().raw() + ".name", warp.getCategory().raw());
            }
        });

        registerWarp("price", handler -> {
            Warp warp = getWarp(handler);
            if (warp == null) return empty;
            double price = warp.getCurrency() == null ? 0 : warp.getTeleportPrice();
            HookManager.CurrencyOptions options = warp.getCurrencyOptions();
            boolean isFree = options == null || warp.getTeleportPrice() == 0;
            return isFree ? LANG.getString("placeholders.free") : options.displayName().replace("%price%", format(price));
        });

        registerWarp("price-full", handler -> {
            Warp warp = getWarp(handler);
            if (warp == null) return empty;
            CurrencyIntegration integration = warp.getCurrencyIntegration();
            if (integration == null) return "0";
            return FormatUtils.formatCurrency(integration, warp.getTeleportPrice());
        });

        registerWarp("access", handler -> {
            Warp warp = getWarp(handler);
            if (warp == null) return empty;
            return LANG.getString("access." + warp.getAccess().name().toLowerCase());
        });

        registerWarp("earned_money", handler -> {
            Warp warp = getWarp(handler);
            if (warp == null) return empty;
            CurrencyIntegration integration = warp.getCurrencyIntegration();
            if (integration == null) return "0";
            double earned = warp.getEarnedMoney();
            return FormatUtils.formatCurrency(integration, earned);
        });

        registerWarp("rating_decimal", handler -> {
            Warp warp = getWarp(handler);
            if (warp == null) return empty;
            float rating = warp.getRating();
            return format(rating);
        });

        registerWarp("rating_stars", handler -> {
            Warp warp = getWarp(handler);
            if (warp == null) return empty;
            float rating = warp.getRating();
            int starAm = Math.round(rating);
            return StarUtils.getFormatted(starAm, 5);
        });

        registerWarp("rating_amount", handler -> {
            Warp warp = getWarp(handler);
            if (warp == null) return empty;
            return String.valueOf(warp.getRatingAmount());
        });

        registerWarp("visitors", handler -> {
            Warp warp = getWarp(handler);
            if (warp == null) return empty;
            return String.valueOf(warp.getVisits());
        });

        registerWarp("visitors_unique", handler -> {
            Warp warp = getWarp(handler);
            if (warp == null) return empty;
            return String.valueOf(warp.getUniqueVisits());
        });

        registerWarp("favorites", handler -> {
            Warp warp = getWarp(handler);
            if (warp == null) return empty;
            return String.valueOf(warp.getFavorites());
        });

        registerWarp("icon", handler -> {
            Warp warp = getWarp(handler);
            if (warp == null) return empty;
            return warp.getIcon().name().toLowerCase();
        });

        registerWarp("blacklisted", handler -> {
            Warp warp = getWarp(handler);
            if (warp == null) return empty;
            return String.valueOf(warp.getAccessList(AccessList.BLACKLIST).size());
        });

        registerWarp("whitelisted", handler -> {
            Warp warp = getWarp(handler);
            if (warp == null) return empty;
            return String.valueOf(warp.getAccessList(AccessList.WHITELIST).size());
        });

        registerWarp("given_rating_decimal", handler -> {
            Player player = handler.resolve(Player.class);
            if (player == null) return empty;
            Warp warp = getWarp(handler);
            if (warp == null) return empty;
            Integer rating = warp.getAllRatings().get(player.getUniqueId());
            return rating == null ? "" : format(rating);
        });

        registerWarp("given_rating_stars", handler -> {
            Player player = handler.resolve(Player.class);
            if (player == null) return empty;
            Warp warp = getWarp(handler);
            if (warp == null) return empty;
            Integer rating = warp.getAllRatings().get(player.getUniqueId());
            return rating == null ? LANG.getString("placeholders.no-rating") : StarUtils.getFormatted(rating, 5);
        });
    }

    @Nullable
    private static Warp getWarp(PlaceholderContext handler) throws PlaceholderException {
        Warp warp = handler.raw(Warp.class);
        if (warp == null) {
            warp = handler.argument("warp");
        }
        return warp;
    }

    private static void registerWarp(String placeholder, ThrowingFunction<PlaceholderContext, String, PlaceholderException> parser) {
        PlaceholderHandler.register(placeholder, parser, false);
        PlaceholderHandler.register("<warp>_" + placeholder, new PlaceholderArguments(warpArg), parser, true);
    }
}
