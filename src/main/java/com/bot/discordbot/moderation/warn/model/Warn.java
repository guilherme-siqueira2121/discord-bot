package com.bot.discordbot.moderation.warn.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Modelo de dados representando um Warn (advertência) no sistema.
 * Esta classe é um POJO (Plain Old Java Object) que mapeia a tabela 'warns' do banco.
 */
public class Warn {

    // ==================== ATRIBUTOS ====================

    private int id;
    private String userId;
    private String moderatorId;
    private String reason;
    private long timestamp;      // quando o warn foi aplicado
    private long expiresAt;      // quando o warn expira

    public Warn() {
    }

    // criação manual
    public Warn(int id, String userId, String moderatorId, String reason,
                long timestamp, long expiresAt) {
        this.id = id;
        this.userId = userId;
        this.moderatorId = moderatorId;
        this.reason = reason;
        this.timestamp = timestamp;
        this.expiresAt = expiresAt;
    }

    // criar novo warn
    public Warn(String userId, String moderatorId, String reason,
                long timestamp, long expiresAt) {
        this.userId = userId;
        this.moderatorId = moderatorId;
        this.reason = reason;
        this.timestamp = timestamp;
        this.expiresAt = expiresAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getModeratorId() {
        return moderatorId;
    }

    public void setModeratorId(String moderatorId) {
        this.moderatorId = moderatorId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    // verifica se o warn já expirou
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    // verifica se o warn ainda está ativo
    public boolean isActive() {
        return !isExpired();
    }

    // retorna quanto tempo falta para o warn expirar
    public long getTimeRemaining() {
        long remaining = expiresAt - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    // retorna a duração total do warn
    public long getDuration() {
        return expiresAt - timestamp;
    }

    // formata o timestamp de criação para leitura humana
    public String getFormattedTimestamp() {
        return formatMillisToDateTime(timestamp);
    }

    // formata o timestamp de expiração para leitura humana
    public String getFormattedExpiresAt() {
        return formatMillisToDateTime(expiresAt);
    }

    // formata tempo restante em formato legível (1d 3h 20m 10s)
    public String getFormattedTimeRemaining() {
        return formatMillisToReadable(getTimeRemaining());
    }

    // ==================== MÉTODOS UTILITÁRIOS ====================

    // converte milissegundos para formato de data/hora legível
    private String formatMillisToDateTime(long millis) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("dd/MM/yyyy HH:mm:ss")
                .withZone(ZoneId.systemDefault());
        return formatter.format(Instant.ofEpochMilli(millis));
    }

    // converte milissegundos para formato legível (1d 3h 20m 10s)
    private String formatMillisToReadable(long millis) {
        if (millis <= 0) return "0s";

        long seconds = millis / 1000;
        long days = seconds / 86400;
        seconds %= 86400;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        sb.append(seconds).append("s");

        return sb.toString().trim();
    }

    // ==================== EQUALS, HASHCODE E TOSTRING ====================

    // dois warns são iguais se tiverem o mesmo ID
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Warn warn = (Warn) o;
        return id == warn.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Warn{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", moderatorId='" + moderatorId + '\'' +
                ", reason='" + reason + '\'' +
                ", timestamp=" + getFormattedTimestamp() +
                ", expiresAt=" + getFormattedExpiresAt() +
                ", active=" + isActive() +
                '}';
    }
}