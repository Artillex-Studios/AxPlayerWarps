package com.artillexstudios.axplayerwarps.utils;

import com.artillexstudios.axintegrations.types.CurrencyIntegration;
import com.artillexstudios.axplayerwarps.hooks.HookManager;
import com.artillexstudios.axplayerwarps.placeholders.WarpPlaceholders;

public class FormatUtils {

    public static String formatCurrency(CurrencyIntegration integration, double amount) {
        String formatted = WarpPlaceholders.format(amount);
        if (integration == null) return formatted;
        HookManager.CurrencyOptions currencyOptions = HookManager.getCurrencyOptions(integration);
        if (currencyOptions == null) return formatted;
        return currencyOptions.displayName().replace("%price%", formatted);
    }
}
