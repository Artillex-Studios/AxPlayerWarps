package com.artillexstudios.axplayerwarps.hooks.currency;

import com.artillexstudios.axapi.libs.boostedyaml.block.implementation.Section;
import com.artillexstudios.axapi.reflection.ClassUtils;
import com.artillexstudios.axintegrations.types.CurrencyIntegration;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlaceholderCurrencyIntegration extends CurrencyIntegration {
    private static final DecimalFormat df = new DecimalFormat("#");
    private final Section section;

    public PlaceholderCurrencyIntegration(String name, Section section) {
        super(name, null);
        this.section = section;
    }

    @Override
    public boolean canLoad() {
        return ClassUtils.INSTANCE.classExists("me.clip.placeholderapi.PlaceholderAPI");
    }

    public String getDisplayName() {
        return section.getString("name", "---");
    }

    @Override
    public boolean worksOffline() {
        return section.getBoolean("works-offline", false);
    }

    @Override
    public boolean usesDecimals() {
        return section.getBoolean("uses-double", false);
    }

    @Override
    public double getBalance(@NotNull Player player) {
        String placeholder = section.getString("settings.raw-placeholder");
        return Double.parseDouble(PlaceholderAPI.setPlaceholders(player, placeholder));
    }

    @Override
    public CompletableFuture<Double> getBalance(@NotNull UUID player) {
        OfflinePlayer pl = Bukkit.getOfflinePlayer(player);
        if (pl.getName() == null) return CompletableFuture.completedFuture(0D);
        String placeholder = section.getString("settings.raw-placeholder");
        return CompletableFuture.completedFuture(Double.parseDouble(PlaceholderAPI.setPlaceholders(pl.getPlayer() == null ? pl : pl.getPlayer(), placeholder)));
    }

    @NotNull
    @Override
    public CompletableFuture<Boolean> giveBalance(@NotNull UUID player, double amount) {
        OfflinePlayer pl = Bukkit.getOfflinePlayer(player);
        if (pl.getName() == null) return CompletableFuture.completedFuture(false);
        String numberValue = parseNumber(amount);
        String placeholder = section.getString("settings.give-command")
                .replace("%amount%", numberValue)
                .replace("%price%", numberValue)
                .replace("%player%", pl.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), placeholder);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> takeBalance(@NotNull UUID player, double amount) {
        OfflinePlayer pl = Bukkit.getOfflinePlayer(player);
        if (pl.getName() == null) return CompletableFuture.completedFuture(false);
        String numberValue = parseNumber(amount);
        String placeholder = section.getString("settings.take-command")
                .replace("%amount%", numberValue)
                .replace("%price%", numberValue)
                .replace("%player%", pl.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), placeholder);
        return CompletableFuture.completedFuture(true);
    }

    private String parseNumber(double amount) {
        return df.format(usesDecimals() ? amount : Math.round(amount));
    }
}