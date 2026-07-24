package com.artillexstudios.axplayerwarps.commands.subcommands;

import com.artillexstudios.axapi.utils.Cooldown;
import com.artillexstudios.axintegrations.types.ProtectionIntegration;
import com.artillexstudios.axplayerwarps.AxPlayerWarps;
import com.artillexstudios.axplayerwarps.api.events.AxPlayerWarpsCreateEvent;
import com.artillexstudios.axplayerwarps.api.events.AxPlayerWarpsPreCreateEvent;
import com.artillexstudios.axplayerwarps.enums.Access;
import com.artillexstudios.axplayerwarps.hooks.HookManager;
import com.artillexstudios.axplayerwarps.hooks.currency.CurrencyHook;
import com.artillexstudios.axplayerwarps.user.Users;
import com.artillexstudios.axplayerwarps.user.WarpUser;
import com.artillexstudios.axplayerwarps.utils.FormatUtils;
import com.artillexstudios.axplayerwarps.utils.SimpleRegex;
import com.artillexstudios.axplayerwarps.utils.WarpNameUtils;
import com.artillexstudios.axplayerwarps.warps.Warp;
import com.artillexstudios.axplayerwarps.warps.WarpManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.CONFIG;
import static com.artillexstudios.axplayerwarps.AxPlayerWarps.MESSAGEUTILS;

public enum Create {
    INSTANCE;

    private final Cooldown<Player> cooldown = Cooldown.create();
    public void execute(Player sender, String warpName) {
        WarpUser user = Users.get(sender);
        long limit = user.getWarpLimit();
        long warps = WarpManager.getWarps().stream().filter(warp -> warp.getOwner().equals(sender.getUniqueId())).count();
        if (limit <= warps) {
            MESSAGEUTILS.sendLang(sender, "errors.limit-reached",
                    Map.of("%current%", "" + warps, "%limit%", "" + limit));
            return;
        }

        Location warpLocation = sender.getLocation();
        if (SimpleRegex.matches(CONFIG.getStringList("disallowed-worlds"), warpLocation.getWorld().getName())) {
            MESSAGEUTILS.sendLang(sender, "errors.disallowed-world");
            return;
        }

        if (!ProtectionIntegration.hasPermission(sender, warpLocation, ProtectionIntegration.Permission.BREAK)) {
            MESSAGEUTILS.sendLang(sender, "errors.cannot-create-here");
            return;
        }

        switch (WarpNameUtils.isAllowed(warpName)) {
            case DISALLOWED -> {
                MESSAGEUTILS.sendLang(sender, "errors.disallowed-name-blacklisted");
                return;
            }
            case CONTAINS_SPACES -> {
                MESSAGEUTILS.sendLang(sender, "errors.disallowed-name-space");
                return;
            }
            case INVALID_LENGTH -> {
                MESSAGEUTILS.sendLang(sender, "errors.disallowed-name-length");
                return;
            }
        }

        Optional<Warp> warpOpt = WarpManager.getWarps().stream().filter(warp -> {
            boolean caseSensitive = CONFIG.getBoolean("warp-naming.case-sensitive", false);
            if (caseSensitive) return warp.getName().equals(warpName);
            else return warp.getName().equalsIgnoreCase(warpName);
        }).findAny();
        if (warpOpt.isPresent()) {
            MESSAGEUTILS.sendLang(sender, "errors.name-exists");
            return;
        }

        Warp warp = new Warp(
                null,
                System.currentTimeMillis(),
                null,
                warpName,
                warpLocation,
                warpLocation.getWorld().getName(),
                null,
                sender.getUniqueId(),
                sender.getName(),
                Access.PUBLIC,
                null,
                0,
                0,
                null
        );

        boolean creationPaid = CONFIG.getBoolean("warp-creation-cost.enabled", false);
        double price = creationPaid ? CONFIG.getDouble("warp-creation-cost.price", 1000) : 0;

        AxPlayerWarpsPreCreateEvent preCreateEvent = new AxPlayerWarpsPreCreateEvent(sender, warp, price);
        Bukkit.getServer().getPluginManager().callEvent(preCreateEvent);
        if (preCreateEvent.isCancelled()) return;
        price = preCreateEvent.getCreationPrice();

        CurrencyHook currencyHook;
        if (creationPaid & price > 0) {
            String currStr = CONFIG.getString("warp-creation-cost.currency", "Experience");
            currencyHook = HookManager.getCurrencyHook(currStr);
            if (currencyHook != null) {
                // not enough balance
                if (currencyHook.getBalance(sender.getUniqueId()) < price) {
                    MESSAGEUTILS.sendLang(sender, "errors.create-not-enough-currency",
                            Map.of("%price%", FormatUtils.formatCurrency(currencyHook, price)));
                    return;
                }
                // confirmation
                if (CONFIG.getBoolean("warp-creation-cost.confirm", true) && !cooldown.hasCooldown(sender)) {
                    cooldown.addCooldown(sender, 10_000L);
                    MESSAGEUTILS.sendLang(sender, "create.confirm",
                            Map.of("%price%", FormatUtils.formatCurrency(currencyHook, price)));
                    return;
                }
                currencyHook.takeBalance(sender.getUniqueId(), price);
            }
        } else {
            currencyHook = null;
        }

        AxPlayerWarpsCreateEvent createEvent = new AxPlayerWarpsCreateEvent(sender, warp, price);
        Bukkit.getServer().getPluginManager().callEvent(createEvent);

        final double finalPrice = price;
        AxPlayerWarps.getThreadedQueue().submit(() -> {
            int id = AxPlayerWarps.getDatabase().createWarp(sender, warpLocation, warpName);
            warp.setId(id);
            MESSAGEUTILS.sendLang(sender, "create.created", Map.of("%warp%", warpName, "%price%", FormatUtils.formatCurrency(currencyHook, finalPrice)));
            WarpManager.getWarps().add(warp);
        });
    }
}