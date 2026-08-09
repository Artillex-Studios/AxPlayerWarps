package com.artillexstudios.axplayerwarps.input;

import com.artillexstudios.axapi.scheduler.Scheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.INPUT;

public class InputListener implements Listener {
    public static Map<Player, Consumer<String>> inputPlayers = new HashMap<>();

    public static Map<Player, Consumer<String>> getInputPlayers() {
        return inputPlayers;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        inputPlayers.remove(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Consumer<String> consumer = inputPlayers.remove(event.getPlayer());
        if (consumer == null) return;
        event.setCancelled(true);
        if (INPUT.getStringList("chat-cancel-words").contains(event.getMessage())) {
            Scheduler.get().run(event.getPlayer(), task -> {
                consumer.accept("");
            }, () -> {});
            return;
        }
        Scheduler.get().run(event.getPlayer(), task -> {
            consumer.accept(event.getMessage());
        }, () -> {});
    }
}
