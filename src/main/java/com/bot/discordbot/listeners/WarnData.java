package com.bot.discordbot.listeners;

public class WarnData {
    public int warns;
    public long lastWarnTimestamp;

    public WarnData(int warns, long lastWarnTimestamp) {
        this.warns = warns;
        this.lastWarnTimestamp = lastWarnTimestamp;
    }
}
