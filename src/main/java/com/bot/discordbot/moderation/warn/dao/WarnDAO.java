package com.bot.discordbot.moderation.warn.dao;

import com.bot.discordbot.database.Database;
import com.bot.discordbot.moderation.warn.model.Warn;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WarnDAO {

    // insere um warn com timestamp e expiresAt
    public static void addWarn(String userId, String moderatorId, String reason, long timestamp, long expiresAt) {
        String sql = "INSERT INTO warns (user_id, moderator_id, reason, timestamp, expires_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, moderatorId);
            ps.setString(3, reason);
            ps.setLong(4, timestamp);
            ps.setLong(5, expiresAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // conta warns ativos que não estão expirados
    public static int countActiveWarns(String userId) {
        String sql = "SELECT COUNT(*) FROM warns WHERE user_id = ? AND expires_at > ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setLong(2, System.currentTimeMillis());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    // retorna listas de warns ativos
    public static List<Warn> getActiveWarns(String userId) {
        String sql = "SELECT id, user_id, moderator_id, reason, timestamp, expires_at FROM warns WHERE user_id = ? AND expires_at > ? ORDER BY timestamp ASC";
        List<Warn> out = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setLong(2, System.currentTimeMillis());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Warn(
                            rs.getInt("id"),
                            rs.getString("user_id"),
                            rs.getString("moderator_id"),
                            rs.getString("reason"),
                            rs.getLong("timestamp"),
                            rs.getLong("expires_at")
                    ));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return out;
    }

    // histórico completo (ativos + expirados)
    public static List<Warn> getWarnHistory(String userId) {
        String sql = "SELECT id, user_id, moderator_id, reason, timestamp, expires_at FROM warns WHERE user_id = ? ORDER BY timestamp DESC";
        List<Warn> out = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Warn(
                            rs.getInt("id"),
                            rs.getString("user_id"),
                            rs.getString("moderator_id"),
                            rs.getString("reason"),
                            rs.getLong("timestamp"),
                            rs.getLong("expires_at")
                    ));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return out;
    }

    public static void removeWarnById(int id) {
        String sql = "DELETE FROM warns WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void clearUserWarns(String userId) {
        String sql = "DELETE FROM warns WHERE user_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // purge de warns expirados
    public static void purgeExpiredWarns() {
        String sql = "DELETE FROM warns WHERE expires_at <= ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
