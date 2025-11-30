package com.bot.discordbot.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Classe responsável por gerenciar a conexão com o banco de dados SQLite.
 * Utiliza o padrão Singleton implícito (cada chamada retorna uma nova conexão).
 */
public class Database {

    private static final String URL = "jdbc:sqlite:./data/moderation.db";

    // obtém uma nova conexão com o banco de dados
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    // teste de conexão
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("❌ Erro ao testar conexão: " + e.getMessage());
            return false;
        }
    }
}