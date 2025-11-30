package com.bot.discordbot.database;

import java.sql.Connection;
import java.sql.Statement;

public class DatabaseSetup {

    public static void initialize() {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            // tabela de WARNs
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS warns (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id TEXT NOT NULL,
                    moderator_id TEXT,
                    reason TEXT,
                    timestamp INTEGER NOT NULL,
                    expires_at INTEGER NOT NULL
                );
            """);

            // tabela de logs (mantive como antes)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    action_type TEXT NOT NULL,
                    user_id TEXT,
                    moderator_id TEXT,
                    details TEXT,
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
                );
            """);

            System.out.println("[DB] Tabelas criadas/verificadas com sucesso!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
