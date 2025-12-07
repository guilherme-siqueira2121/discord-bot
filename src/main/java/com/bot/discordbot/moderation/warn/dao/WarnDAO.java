package com.bot.discordbot.moderation.warn.dao;

import com.bot.discordbot.database.Database;
import com.bot.discordbot.moderation.warn.model.Warn;
import com.bot.discordbot.util.BotLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object para operações relacionadas a warns no banco de dados
 */
public class WarnDAO {

    // ==================== SQL QUERIES ====================

    private static final String INSERT_WARN =
            "INSERT INTO warns (user_id, moderator_id, reason, timestamp, expires_at) VALUES (?, ?, ?, ?, ?)";

    private static final String COUNT_ACTIVE_WARNS =
            "SELECT COUNT(*) FROM warns WHERE user_id = ? AND expires_at > ?";

    private static final String SELECT_ACTIVE_WARNS =
            "SELECT id, user_id, moderator_id, reason, timestamp, expires_at " +
                    "FROM warns WHERE user_id = ? AND expires_at > ? ORDER BY timestamp ASC";

    private static final String SELECT_WARN_HISTORY =
            "SELECT id, user_id, moderator_id, reason, timestamp, expires_at " +
                    "FROM warns WHERE user_id = ? ORDER BY timestamp DESC";

    private static final String SELECT_WARN_BY_ID =
            "SELECT id, user_id, moderator_id, reason, timestamp, expires_at " +
                    "FROM warns WHERE id = ?";

    private static final String DELETE_WARN_BY_ID =
            "DELETE FROM warns WHERE id = ?";

    private static final String DELETE_USER_WARNS =
            "DELETE FROM warns WHERE user_id = ?";

    private static final String DELETE_EXPIRED_WARNS =
            "DELETE FROM warns WHERE expires_at <= ?";

    // ==================== CREATE ====================

    /**
     * Insere um warn no banco de dados
     * @return true se inserção deu certo
     */
    public static boolean addWarn(String userId, String moderatorId, String reason,
                                  long timestamp, long expiresAt) {
        if (userId == null || userId.isEmpty()) {
            BotLogger.error("Tentativa de adicionar warn com userId inválido");
            return false;
        }

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_WARN)) {

            ps.setString(1, userId);
            ps.setString(2, moderatorId);
            ps.setString(3, reason);
            ps.setLong(4, timestamp);
            ps.setLong(5, expiresAt);

            int rowsAffected = ps.executeUpdate();
            boolean success = rowsAffected > 0;

            if (success) {
                BotLogger.database("INSERT warn para user " + userId, true);
            } else {
                BotLogger.warn("INSERT warn não afetou nenhuma linha");
            }

            return success;

        } catch (SQLException e) {
            BotLogger.error("Erro ao adicionar warn", e);
            return false;
        }
    }

    // ==================== READ ====================

    /**
     * Conta warns ativos de um usuário
     * @return número de warns ativos, ou 0 caso dê erro
     */
    public static int countActiveWarns(String userId) {
        if (userId == null || userId.isEmpty()) {
            BotLogger.warn("userId nulo ou vazio em countActiveWarns");
            return 0;
        }

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(COUNT_ACTIVE_WARNS)) {

            ps.setString(1, userId);
            ps.setLong(2, System.currentTimeMillis());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    BotLogger.debug("User " + userId + " tem " + count + " warns ativos");
                    return count;
                }
            }

        } catch (SQLException e) {
            BotLogger.error("Erro ao contar warns ativos", e);
        }

        return 0;
    }

    /**
     * Retorna lista de warns ativos de um usuário
     * @return lista de warns (vazia se nenhum ou erro)
     */
    public static List<Warn> getActiveWarns(String userId) {
        List<Warn> warns = new ArrayList<>();

        if (userId == null || userId.isEmpty()) {
            BotLogger.warn("userId nulo ou vazio em getActiveWarns");
            return warns;
        }

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ACTIVE_WARNS)) {

            ps.setString(1, userId);
            ps.setLong(2, System.currentTimeMillis());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    warns.add(mapResultSetToWarn(rs));
                }
            }

            BotLogger.debug("Carregados " + warns.size() + " warns ativos para " + userId);

        } catch (SQLException e) {
            BotLogger.error("Erro ao buscar warns ativos", e);
        }

        return warns;
    }

    /**
     * Retorna histórico completo de warns de um usuário estejam eles ativos ou expirados
     * @return lista de warns (vazia se não houver nenhum ou der erro)
     */
    public static List<Warn> getWarnHistory(String userId) {
        List<Warn> warns = new ArrayList<>();

        if (userId == null || userId.isEmpty()) {
            BotLogger.warn("userId nulo ou vazio em getWarnHistory");
            return warns;
        }

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_WARN_HISTORY)) {

            ps.setString(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    warns.add(mapResultSetToWarn(rs));
                }
            }

            BotLogger.debug("Carregado histórico de " + warns.size() + " warns para " + userId);

        } catch (SQLException e) {
            BotLogger.error("Erro ao buscar histórico de warns", e);
        }

        return warns;
    }

    /**
     * Busca um warn específico por ID
     */
    public static Optional<Warn> getWarnById(int id) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_WARN_BY_ID)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToWarn(rs));
                }
            }

        } catch (SQLException e) {
            BotLogger.error("Erro ao buscar warn por ID " + id, e);
        }

        return Optional.empty();
    }

    // ==================== DELETE ====================

    /**
     * Remove um warn específico por ID
     */
    public static boolean removeWarnById(int id) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_WARN_BY_ID)) {

            ps.setInt(1, id);
            int rowsAffected = ps.executeUpdate();
            boolean success = rowsAffected > 0;

            if (success) {
                BotLogger.database("DELETE warn ID " + id, true);
            } else {
                BotLogger.warn("Warn ID " + id + " não foi encontrado para remoção");
            }

            return success;

        } catch (SQLException e) {
            BotLogger.error("Erro ao remover warn por ID", e);
            return false;
        }
    }

    /**
     * remove todos os warns de um usuário
     */
    public static int clearUserWarns(String userId) {
        if (userId == null || userId.isEmpty()) {
            BotLogger.warn("userId nulo ou vazio em clearUserWarns");
            return 0;
        }

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_USER_WARNS)) {

            ps.setString(1, userId);
            int rowsAffected = ps.executeUpdate();

            BotLogger.database("DELETE " + rowsAffected + " warns do user " + userId, true);
            return rowsAffected;

        } catch (SQLException e) {
            BotLogger.error("Erro ao limpar warns do usuário", e);
            return 0;
        }
    }

    /**
     * Remove todos os warns expirados do banco
     */
    public static int purgeExpiredWarns() {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_EXPIRED_WARNS)) {

            ps.setLong(1, System.currentTimeMillis());
            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                BotLogger.database("PURGE de " + rowsAffected + " warns expirados", true);
            }

            return rowsAffected;

        } catch (SQLException e) {
            BotLogger.error("Erro ao purgar warns expirados", e);
            return 0;
        }
    }

    // ==================== UTILITIES ====================

    /**
     * Mapeia um ResultSet para um objeto warn
     */
    private static Warn mapResultSetToWarn(ResultSet rs) throws SQLException {
        return new Warn(
                rs.getInt("id"),
                rs.getString("user_id"),
                rs.getString("moderator_id"),
                rs.getString("reason"),
                rs.getLong("timestamp"),
                rs.getLong("expires_at")
        );
    }

    /**
     * Retorna estatísticas gerais de warns
     */
    public static String getStatistics() {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            var rs = stmt.executeQuery(
                    "SELECT " +
                            "COUNT(*) as total, " +
                            "SUM(CASE WHEN expires_at > " + System.currentTimeMillis() + " THEN 1 ELSE 0 END) as active, " +
                            "COUNT(DISTINCT user_id) as unique_users " +
                            "FROM warns"
            );

            if (rs.next()) {
                return String.format(
                        "📊 Estatísticas: %d warns total | %d ativos | %d usuários únicos",
                        rs.getInt("total"),
                        rs.getInt("active"),
                        rs.getInt("unique_users")
                );
            }

        } catch (SQLException e) {
            BotLogger.error("Erro ao obter estatísticas", e);
        }

        return "❌ Erro ao obter estatísticas";
    }
}