package com.artillexstudios.axplayerwarps.input;

import com.artillexstudios.axapi.scheduler.Scheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.INPUT;

public class InputListener implements Listener {
    public static final ConcurrentHashMap<Player, Consumer<String>> inputPlayers = new ConcurrentHashMap<>();

    public static Map<Player, Consumer<String>> getInputPlayers() {
        return inputPlayers;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Consumer<String> consumer = inputPlayers.remove(event.getPlayer());
        if (consumer == null) return;
        event.setCancelled(true);
        if (INPUT.getStringList("chat-cancel-words").contains(event.getMessage())) {
            Scheduler.get().run(() -> consumer.accept(""));
            return;
        }
        Scheduler.get().run(() -> consumer.accept(event.getMessage()));
    }
}
