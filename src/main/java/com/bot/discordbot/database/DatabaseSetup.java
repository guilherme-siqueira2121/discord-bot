package com.bot.discordbot.database;

import com.bot.discordbot.util.BotLogger;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

/**
 * Classe responsável por criar as tabelas do banco de dados.
 */
public class DatabaseSetup {

    /**
     * Inicializa todas as tabelas necessárias
     */
    public static void initialize() {
        BotLogger.info("Criando/verificando tabelas do banco de dados...");

        // usa apneas uma conexão para todas as operações
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            // desabilita autocommit para fazer tudo em uma transação
            conn.setAutoCommit(false);

            try {
                // tabela warns
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS warns (
                        id SERIAL PRIMARY KEY,
                        user_id VARCHAR(20) NOT NULL,
                        moderator_id VARCHAR(20),
                        reason TEXT,
                        timestamp BIGINT NOT NULL,
                        expires_at BIGINT NOT NULL
                    );
                """);
                BotLogger.debug("Tabela 'warns' criada/verificada");

                // tabela logs
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS logs (
                        id SERIAL PRIMARY KEY,
                        action_type VARCHAR(50) NOT NULL,
                        user_id VARCHAR(20),
                        moderator_id VARCHAR(20),
                        details TEXT,
                        timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    );
                """);
                BotLogger.debug("Tabela 'logs' criada/verificada");

                // índices
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_warns_user_id ON warns(user_id)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_warns_expires_at ON warns(expires_at)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_warns_user_expires ON warns(user_id, expires_at)");
                BotLogger.debug("Índices criados/verificados");

                // commit tudo de uma vez
                conn.commit();

                BotLogger.success("✅ Tabelas e índices criados com sucesso!");

            } catch (SQLException e) {
                // se der erro, faz rollback
                conn.rollback();
                BotLogger.error("Erro ao criar tabelas, rollback executado", e);
                throw e;
            }

        } catch (Exception e) {
            BotLogger.error("Erro crítico ao inicializar banco de dados", e);
            throw new RuntimeException("Falha ao inicializar tabelas do banco", e);
        }
    }

    /**
     * Verifica integridade da database
     */
    public static boolean verifyDatabase() {
        BotLogger.info("Verificando integridade do banco de dados...");

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            // PostgreSQL: usar information_schema em vez de sqlite_master
            var rs = stmt.executeQuery(
                    "SELECT table_name FROM information_schema.tables " +
                            "WHERE table_schema = 'public' AND table_name IN ('warns', 'logs')"
            );

            int tableCount = 0;
            while (rs.next()) {
                tableCount++;
                BotLogger.debug("Tabela encontrada: " + rs.getString("table_name"));
            }

            if (tableCount == 2) {
                BotLogger.success("✅ Todas as tabelas estão presentes");
                return true;
            } else {
                BotLogger.error("❌ Faltam tabelas no banco de dados (encontradas: " + tableCount + "/2)");
                return false;
            }

        } catch (SQLException e) {
            BotLogger.error("Erro ao verificar banco de dados", e);
            return false;
        }
    }

    /**
     * Reseta o database
     */
    public static void resetDatabase() {
        BotLogger.warn("⚠️ RESETANDO BANCO DE DADOS - TODOS OS DADOS SERÃO PERDIDOS!");

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            // desabilita autocommit
            conn.setAutoCommit(false);

            try {
                stmt.execute("DROP TABLE IF EXISTS warns");
                stmt.execute("DROP TABLE IF EXISTS logs");

                conn.commit();

                BotLogger.info("Tabelas removidas. Recriando...");

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (Exception e) {
            BotLogger.error("Erro ao resetar banco de dados", e);
            throw new RuntimeException("Falha ao resetar banco", e);
        }

        // recria
        initialize();

        BotLogger.success("Banco de dados resetado com sucesso!");
    }
}