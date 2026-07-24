package com.artillexstudios.axplayerwarps.hooks.currency;

import com.artillexstudios.axapi.utils.StringUtils;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellenteconomy.api.ExcellentEconomyAPI;
import su.nightexpress.excellenteconomy.api.currency.ExcellentCurrency;

import java.util.UUID;

public class ExcellentEconomyHook implements CurrencyHook {
    private ExcellentEconomyAPI api;
    private ExcellentCurrency currency = null;
    private final String internal;
    private final String name;

    public ExcellentEconomyHook(String internal, String name) {
        this.internal = internal;
        this.name = name;
    }

    @Override
    public void setup() {
        api = Bukkit.getServer().getServicesManager().getRegistration(ExcellentEconomyAPI.class).getProvider();
        currency = api.getCurrency(internal);
        if (currency == null) {
            Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#FF0000[AxPlayerWarps] ExcellentEconomy currency named &#DD0000" + internal + " &#FF0000not found! Change the currency-name or disable the hook to get rid of this warning!"));
        }
    }

    @Override
    public String getName() {
        return "ExcellentEconomy-" + internal;
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public boolean worksOffline() {
        return false;
    }

    @Override
    public boolean usesDouble() {
        return true;
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public double getBalance(@NotNull UUID player) {
        if (currency == null) return 0;
        return api.getBalance(Bukkit.getPlayer(player), currency);
    }

    @Override
    public void giveBalance(@NotNull UUID player, double amount) {
        if (currency == null) return;
        api.deposit(Bukkit.getPlayer(player), currency, amount);
    }

    @Override
    public void takeBalance(@NotNull UUID player, double amount) {
        if (currency == null) return;
        api.withdraw(Bukkit.getPlayer(player), currency, amount);
    }
}