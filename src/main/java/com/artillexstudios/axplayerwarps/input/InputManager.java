package com.artillexstudios.axplayerwarps.input;

import com.artillexstudios.axapi.gui.AnvilInput;
import com.artillexstudios.axapi.gui.SignInput;
import com.artillexstudios.axapi.items.WrappedItemStack;
import com.artillexstudios.axapi.libs.boostedyaml.block.implementation.Section;
import com.artillexstudios.axapi.scheduler.Scheduler;
import com.artillexstudios.axapi.utils.ItemBuilder;
import com.artillexstudios.axapi.utils.StringUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.INPUT;

public class InputManager {

    public static void getInput(Player player, String route, Consumer<String> consumer) {
        Section section = INPUT.getSection(route);
        switch (InputType.valueOf(section.getString("type").toUpperCase(Locale.ENGLISH))) {
            case SIGN -> openSign(player, section, consumer);
            case ANVIL -> openAnvil(player, section, consumer);
            case CHAT -> openChat(player, section, consumer);
        }
    }

    private static void openSign(Player player, Section section, Consumer<String> consumer) {
        new SignInput(player, StringUtils.formatList(section.getStringList("sign")).toArray(Component[]::new),(player1, result) -> {
            try {
                String res = result[0];
                if (res.isBlank()) {
                    consumer.accept("");
                    return;
                }
                Scheduler.get().run(() -> consumer.accept(res));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).open();
    }

    private static void openAnvil(Player player, Section section, Consumer<String> consumer) {
        AtomicBoolean ended = new AtomicBoolean(false);
        new AnvilInput(
                player,
                WrappedItemStack.wrap(ItemBuilder.create(section.getSection("anvil.item")).get()),
                StringUtils.format(section.getString("anvil.title")),
                clickEvent -> {
                    clickEvent.setCancelled(true);
                    if (ended.get()) return;
                    if (clickEvent.getSlot() != 2) return;
                    ItemStack item = clickEvent.getInventory().getItem(2);
                    if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;
                    String res = item.getItemMeta().getDisplayName();
                    clickEvent.getInventory().clear();
                    Scheduler.get().run(() -> consumer.accept(res));
                    ended.set(true);
                },
                closeEvent -> {
                    closeEvent.getInventory().clear();
                    if (ended.get()) return;
                    Scheduler.get().run(() -> consumer.accept(""));
                }
        ).open();
    }

    private static void openChat(Player player, Section section, Consumer<String> consumer) {
        Scheduler.get().runAt(player.getLocation(), player::closeInventory);
        player.sendMessage(String.join("\n", StringUtils.formatListToString(section.getStringList("chat"))));
        InputListener.getInputPlayers().put(player, consumer);
    }
}
