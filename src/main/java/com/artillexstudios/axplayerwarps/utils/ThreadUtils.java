package com.artillexstudios.axplayerwarps.utils;

import org.bukkit.Bukkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadUtils {
    private static final Logger log = LoggerFactory.getLogger(ThreadUtils.class);

    public static void checkMain(String message) {
        if (!Bukkit.isPrimaryThread()) {
            log.error("Thread {} failed main thread check for {}!", Thread.currentThread().getName(), message, new Throwable());
            throw new RuntimeException();
        }
    }

    public static void checkNotMain(String message) {
        if (Bukkit.isPrimaryThread()) {
            log.error("Thread {} failed main thread check for {}!", Thread.currentThread().getName(), message, new Throwable());
            throw new RuntimeException();
        }
    }
}
