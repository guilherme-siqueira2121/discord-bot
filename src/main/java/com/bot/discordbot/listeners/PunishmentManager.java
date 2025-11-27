package com.bot.discordbot.listeners;

import java.util.HashMap;
import java.util.Map;

public class PunishmentManager {

    // armazena warns: userId → quantidade
    private static final Map<String, Integer> warns = new HashMap<>();

    public static int addWarn(String userId) {
        int current = warns.getOrDefault(userId, 0) + 1;
        warns.put(userId, current);
        return current;
    }

    public static int getWarns(String userId) {
        return warns.getOrDefault(userId, 0);
    }

    public static void resetWarns(String userId) {
        warns.put(userId, 0);
    }
}