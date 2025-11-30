package com.bot.discordbot.moderation.warn.service;

import com.bot.discordbot.moderation.warn.dao.WarnDAO;
import com.bot.discordbot.moderation.warn.model.Warn;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.time.Duration;
import java.util.List;

public class WarnService {

    // adiciona um warn, calcula expiresAt e aplica punição automática
    public static void addWarn(String userId, String moderatorId, String reason, Guild guild) {
        // limpa warns expirados no banco (opcional)
        WarnDAO.purgeExpiredWarns();

        // verifica quantos warns ativos existem atualmente
        int current = WarnDAO.countActiveWarns(userId);
        int newCount = current + 1;

        long now = System.currentTimeMillis();
        long expiresAt = calculateExpirationMillis(newCount, now);

        // persiste
        WarnDAO.addWarn(userId, moderatorId, reason, now, expiresAt);

        // aplica punição com base no newCount
        applyPunishmentIfNeeded(newCount, userId, guild);
    }

    public static List<Warn> getActiveWarns(String userId) {
        return WarnDAO.getActiveWarns(userId);
    }

    public static List<Warn> getWarnHistory(String userId) {
        return WarnDAO.getWarnHistory(userId);
    }

    public static void clearUserWarns(String userId) {
        WarnDAO.clearUserWarns(userId);
    }

    // regras de expiração
    private static long calculateExpirationMillis(int warnsCount, long referenceTime) {
        // 1 -> 24h
        // 2 -> 48h
        // 3 -> 7 days
        // 4 -> 14 days
        // 5 -> 30 days
        // 6 -> 365 days (practically permanent)
        switch (warnsCount) {
            case 1: return referenceTime + Duration.ofHours(24).toMillis();
            case 2: return referenceTime + Duration.ofHours(48).toMillis();
            case 3: return referenceTime + Duration.ofDays(7).toMillis();
            case 4: return referenceTime + Duration.ofDays(14).toMillis();
            case 5: return referenceTime + Duration.ofDays(30).toMillis();
            default: return referenceTime + Duration.ofDays(365).toMillis();
        }
    }

    // -------------------------
    // punições
    // 1 -> aviso
    // 2 -> timeout 10m
    // 3 -> timeout 1h
    // 4 -> mute 24h
    // 5 -> mute 3 dias (aviso que o próximo é ban)
    // 6 -> ban
    // -------------------------
    private static void applyPunishmentIfNeeded(int warns, String targetUserId, Guild guild) {
        if (guild == null) return;

        Member member = guild.getMemberById(targetUserId);
        if (member == null) return;
        if (member.getUser().isBot()) return; // não punir bots
        // Não punir se membro tem permissão de mod (safety)
        if (member.hasPermission(net.dv8tion.jda.api.Permission.MODERATE_MEMBERS) ||
                member.hasPermission(net.dv8tion.jda.api.Permission.KICK_MEMBERS) ||
                member.hasPermission(net.dv8tion.jda.api.Permission.BAN_MEMBERS)) {
            return;
        }
    }
}
