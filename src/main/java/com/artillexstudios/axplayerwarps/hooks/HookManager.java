package com.artillexstudios.axplayerwarps.hooks;

import com.artillexstudios.axapi.libs.boostedyaml.block.implementation.Section;
import com.artillexstudios.axintegrations.IntegrationManager;
import com.artillexstudios.axintegrations.IntegrationSetup;
import com.artillexstudios.axintegrations.IntegrationType;
import com.artillexstudios.axintegrations.api.AxIntegrationsAPI;
import com.artillexstudios.axintegrations.types.CurrencyIntegration;
import com.artillexstudios.axplayerwarps.hooks.currency.PlaceholderCurrencyIntegration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.CURRENCIES;
import static com.artillexstudios.axplayerwarps.AxPlayerWarps.HOOKS;

public class HookManager {
    private static final Map<String, CurrencyOptions> currencyOptions = new HashMap<>();
    private static final List<CurrencyIntegration> placeholderCurrencyIntegrations = new ArrayList<>();

    public record CurrencyOptions(String displayName) {}

    public static void setupHooks() {

        IntegrationSetup.builder()
                .enableCurrencyIntegrations(name -> {
                    Section currencies = CURRENCIES.getSection("currencies");
                    return currencies.getBoolean("%s.register".formatted(name), true);
                }, name -> {
                    Section currencies = CURRENCIES.getSection("currencies");
                    List<Map<?, ?>> list = currencies.getMapList("%s.enabled".formatted(name));
                    if (list.isEmpty()) { // this should never be called
                        String displayName = currencies.getString("%s.name".formatted(name));
                        currencyOptions.put(name, new CurrencyOptions(displayName));
                        return Collections.singletonList("");
                    }
                    Set<String> set = new HashSet<>();
                    for (Map<?, ?> map : list) {
                        String currencyName = (String) map.get("currency-name");
                        String displayName = (String) map.get("name");
                        set.add(currencyName);
                        currencyOptions.put("%s-%s".formatted(name, currencyName), new CurrencyOptions(displayName));
                    }
                    return new ArrayList<>(set);
                })
                .enableProtectionIntegrations(name -> {
                    return HOOKS.getBoolean("hooks.protection.%s".formatted(name), true);
                })
                .runAfterLoad(() -> {
                    {
                        boolean modified = false;
                        for (String name : IntegrationManager.listAvailableIntegrations(IntegrationType.PROTECTION).keySet()) {
                            String route = "%s.%s".formatted("hooks.protection", name);
                            if (HOOKS.getString(route) == null) {
                                HOOKS.set(route, true);
                                modified = true;
                            }
                        }
                        if (modified) HOOKS.save();
                    }
                    {
                        boolean modified = false;
                        Map<String, Boolean> currencyIntegrations = new HashMap<>();
                        for (String name : IntegrationManager.listAvailableIntegrations(IntegrationType.CURRENCY).values()) {
                            String[] st = name.split("-");
                            currencyIntegrations.put(st[0], st.length > 1);
                        }
                        for (Map.Entry<String, Boolean> entry : currencyIntegrations.entrySet()) {
                            String plugin = entry.getKey();
                            boolean multiCurrency = entry.getValue();
                            String route = "currencies.%s".formatted(plugin);
                            if (CURRENCIES.getSection(route) == null) {
                                CURRENCIES.set("%s.register".formatted(route), true);
                                if (!multiCurrency) {
                                    CURRENCIES.set("%s.name".formatted(route), "%price% money");
                                } else {
                                    List<Map<String, String>> mapList = new ArrayList<>();
                                    Map<String, String> map = new HashMap<>();
                                    map.put("currency-name", "coins");
                                    map.put("name", "%price% coins");
                                    mapList.add(map);
                                    CURRENCIES.set("%s.enabled".formatted(route), mapList);
                                }
                                modified = true;
                            }
                        }
                        if (modified) CURRENCIES.save();
                    }

                    registerPlaceholderCurrencies();
                })
                .runAfterSetup(() -> {
                    for (CurrencyIntegration integration : CurrencyIntegration.list()) {
                        if (integration instanceof PlaceholderCurrencyIntegration) continue;
                        String name = integration.getName();
                        if (!integration.getFormattedName().equals(integration.getName())) continue;
                        currencyOptions.put(name, new CurrencyOptions(CURRENCIES.getString("currencies.%s.name".formatted(name))));
                    }
                })
                .setup();
    }

    public static void updateHooks() {
        IntegrationManager.reload(() -> {
            for (CurrencyIntegration integration : placeholderCurrencyIntegrations) {
                AxIntegrationsAPI.unregisterIntegration(integration);
                currencyOptions.remove(integration.getFormattedName());
            }
            registerPlaceholderCurrencies();
        });
    }

    private static void registerPlaceholderCurrencies() {
        Section placeholderCurrencies = CURRENCIES.getSection("placeholder-currencies");
        for (String route : placeholderCurrencies.getRoutesAsStrings(false)) {
            Section section = placeholderCurrencies.getSection(route);
            if (!section.getBoolean("register", false)) continue;
            PlaceholderCurrencyIntegration integration = new PlaceholderCurrencyIntegration(route, section);
            if (!integration.canLoad()) continue;
            if (!integration.setup()) continue;
            AxIntegrationsAPI.registerIntegration(integration);
            placeholderCurrencyIntegrations.add(integration);
            String val = section.getString("name", "%price% money");
            currencyOptions.put(route, new CurrencyOptions(val));
        }
    }

    public static CurrencyOptions getCurrencyOptions(CurrencyIntegration integration) {
        return currencyOptions.get(integration.getFormattedName());
    }
}