package com.artillexstudios.axplayerwarps.hooks;

import com.artillexstudios.axapi.reflection.ClassUtils;
import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axintegrations.IntegrationManager;
import com.artillexstudios.axintegrations.IntegrationSetup;
import com.artillexstudios.axintegrations.IntegrationType;
import com.artillexstudios.axplayerwarps.hooks.currency.AxQuestBoardHook;
import com.artillexstudios.axplayerwarps.hooks.currency.BeastTokensHook;
import com.artillexstudios.axplayerwarps.hooks.currency.CoinsEngineHook;
import com.artillexstudios.axplayerwarps.hooks.currency.CurrencyHook;
import com.artillexstudios.axplayerwarps.hooks.currency.EcoBitsHook;
import com.artillexstudios.axplayerwarps.hooks.currency.ExcellentEconomyHook;
import com.artillexstudios.axplayerwarps.hooks.currency.ExperienceHook;
import com.artillexstudios.axplayerwarps.hooks.currency.KingdomsXHook;
import com.artillexstudios.axplayerwarps.hooks.currency.PlaceholderCurrencyHook;
import com.artillexstudios.axplayerwarps.hooks.currency.PlayerPointsHook;
import com.artillexstudios.axplayerwarps.hooks.currency.RedisEconomyHook;
import com.artillexstudios.axplayerwarps.hooks.currency.RivalHarvesterHoesHook;
import com.artillexstudios.axplayerwarps.hooks.currency.RoyaleEconomyHook;
import com.artillexstudios.axplayerwarps.hooks.currency.SuperMobCoinsHook;
import com.artillexstudios.axplayerwarps.hooks.currency.TheOnlyMobCoins;
import com.artillexstudios.axplayerwarps.hooks.currency.TokenManagerHook;
import com.artillexstudios.axplayerwarps.hooks.currency.UltraEconomyHook;
import com.artillexstudios.axplayerwarps.hooks.currency.VaultHook;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Map;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.CURRENCIES;
import static com.artillexstudios.axplayerwarps.AxPlayerWarps.HOOKS;

public class HookManager {
    private static final ArrayList<CurrencyHook> currency = new ArrayList<>();

    public static void setupHooks() {
        IntegrationSetup.builder()
                .enableProtectionIntegrations(name -> {
                    return HOOKS.getBoolean("hooks.protection." + name, true);
                })
                .runAfterLoad(() -> {
                    boolean modified = false;
                    for (String name : IntegrationManager.listAvailableIntegrations(IntegrationType.PROTECTION).keySet()) {
                        String route = "%s.%s".formatted("hooks.protection", name);
                        if (HOOKS.getString(route) == null) {
                            HOOKS.set(route, true);
                            modified = true;
                        }
                    }
                    if (modified) HOOKS.save();
                })
                .runAfterSetup(() -> {

                })
                .setup();
        updateHooks(true);
    }

    public static void updateHooks(boolean setup) {
        currency.removeIf(currencyHook -> !currencyHook.isPersistent());
        if (!setup) IntegrationManager.reload();

        if (CURRENCIES.getBoolean("currencies.Experience.register", true))
            currency.add(new ExperienceHook());

        if (CURRENCIES.getBoolean("currencies.Vault.register", true) && Bukkit.getPluginManager().getPlugin("Vault") != null) {
            currency.add(new VaultHook());
            Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxPlayerWarps] Hooked into Vault!"));
        }

        if (CURRENCIES.getBoolean("currencies.PlayerPoints.register", true) && Bukkit.getPluginManager().getPlugin("PlayerPoints") != null) {
            currency.add(new PlayerPointsHook());
            Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxPlayerWarps] Hooked into PlayerPoints!"));
        }

        if (ClassUtils.INSTANCE.classExists("su.nightexpress.excellenteconomy.api.ExcellentEconomyAPI")) {
            if (CURRENCIES.getBoolean("currencies.ExcellentEconomy.register", true)) {
                for (Map<Object, Object> curr : CURRENCIES.getMapList("currencies.ExcellentEconomy.enabled")) {
                    currency.add(new ExcellentEconomyHook((String) curr.get("currency-name"), (String) curr.get("name")));
                }
                Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxPlayerWarps] Hooked into CoinsEngine!"));
            }
        } else {
            if (CURRENCIES.getBoolean("currencies.CoinsEngine.register", true) && Bukkit.getPluginManager().getPlugin("CoinsEngine") != null) {
                for (Map<Object, Object> curr : CURRENCIES.getMapList("currencies.CoinsEngine.enabled")) {
                    currency.add(new CoinsEngineHook((String) curr.get("currency-name"), (String) curr.get("name")));
                }
                Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxPlayerWarps] Hooked into CoinsEngine!"));
            }
        }

        if (CURRENCIES.getBoolean("currencies.RoyaleEconomy.register", true) && Bukkit.getPluginManager().getPlugin("RoyaleEconomy") != null) {
            currency.add(new RoyaleEconomyHook());
            Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxPlayerWarps] Hooked into RoyaleEconomy!"));
        }

        if (CURRENCIES.getBoolean("currencies.UltraEconomy.register", true) && Bukkit.getPluginManager().getPlugin("UltraEconomy") != null) {
            for (Map<Object, Object> curr : CURRENCIES.getMapList("currencies.UltraEconomy.enabled")) {
                currency.add(new UltraEconomyHook((String) curr.get("currency-name"), (String) curr.get("name")));
            }
            Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxPlayerWarps] Hooked into UltraEconomy!"));
        }

        if (CURRENCIES.getBoolean("currencies.KingdomsX.register", true) && Bukkit.getPluginManager().getPlugin("Kingdoms") != null) {
            currency.add(new KingdomsXHook());
            Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxPlayerWarps] Hooked into KingdomsX!"));
        }

        if (CURRENCIES.getBoolean("currencies.RivalHarvesterHoes.register", true) && Bukkit.getPluginManager().getPlugin("RivalHarvesterHoes") != null) {
            currency.add(new RivalHarvesterHoesHook());
            Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxPlayerWarps] Hooked into RivalHarvesterHoes!"));
        }

        if (CURRENCIES.getBoolean("currencies.SuperMobCoins.register", true) && Bukkit.getPluginManager().getPlugin("SuperMobCoins") != null) {
            currency.add(new SuperMobCoinsHook());
            Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxPlayerWarps] Hooked into SuperMobCoins!"));
        }

        if (CURRENCIES.getBoolean("currencies.TheOnly-MobCoins.register", true) && Bukkit.getPluginManager().getPlugin("TheOnly-MobCoins") != null) {
            currency.add(new TheOnlyMobCoins());
            Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxPlayerWarps] Hooked into TheOnly-MobCoins!"));
        }

        if (CURRENCIES.getBoolean("currencies.TokenManager.register", true) && Bukkit.getPluginManager().getPlugin("TokenManager") != null) {
            currency.add(new TokenManagerHook());
            Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxPlayerWarps] Hooked into TokenManager!"));
        }

        if (CURRENCIES.getBoolean("currencies.AxQuestBoard.register", true) && Bukkit.getPluginManager().getPlugin("AxQuestBoard") != null) {
            currency.add(new AxQuestBoardHook());
            Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxPlayerWarps] Hooked into AxQuestBoard!"));
        }

        if (CURRENCIES.getBoolean("currencies.RedisEconomy.register", true) && Bukkit.getPluginManager().getPlugin("RedisEconomy") != null) {
            for (Map<Object, Object> curr : CURRENCIES.getMapList("currencies.RedisEconomy.enabled")) {
                currency.add(new RedisEconomyHook((String) curr.get("currency-name"), (String) curr.get("name")));
            }
            Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxPlayerWarps] Hooked into RedisEconomy!"));
        }

        if (CURRENCIES.getBoolean("currencies.BeastTokens.register", true) && Bukkit.getPluginManager().getPlugin("BeastTokens") != null) {
            currency.add(new BeastTokensHook());
            Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxPlayerWarps] Hooked into BeastTokens!"));
        }

        if (CURRENCIES.getBoolean("currencies.EcoBits.register", true) && Bukkit.getPluginManager().getPlugin("EcoBits") != null) {
            for (Map<Object, Object> curr : CURRENCIES.getMapList("currencies.EcoBits.enabled")) {
                currency.add(new EcoBitsHook((String) curr.get("currency-name"), (String) curr.get("name")));
            }
            Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxPlayerWarps] Hooked into EcoBits!"));
        }

        for (String str : CURRENCIES.getSection("placeholder-currencies").getRoutesAsStrings(false)) {
            if (!CURRENCIES.getBoolean("placeholder-currencies." + str + ".register", false)) continue;
            currency.add(new PlaceholderCurrencyHook(str, CURRENCIES.getSection("placeholder-currencies." + str)));
            Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxPlayerWarps] Loaded placeholder currency " + str + "!"));
        }

        for (CurrencyHook hook : currency) hook.setup();
    }

    @SuppressWarnings("unused")
    public static void registerCurrencyHook(@NotNull Plugin plugin, @NotNull CurrencyHook currencyHook) {
        currency.add(currencyHook);
        Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxPlayerWarps] Hooked into " + plugin.getName() + "! Note: You must set the currency provider to CUSTOM or it will be overridden after reloading!"));
    }

    @NotNull
    public static ArrayList<CurrencyHook> getCurrency() {
        return currency;
    }

    @Nullable
    public static CurrencyHook getCurrencyHook(@NotNull String name) {
        for (CurrencyHook hook : currency) {
            if (!hook.getName().equals(name)) continue;
            return hook;
        }

        return null;
    }
}
