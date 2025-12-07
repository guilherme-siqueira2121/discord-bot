package com.bot.discordbot.database;

import com.bot.discordbot.config.BotConfig;
import com.bot.discordbot.util.BotLogger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * classe responsável por gerenciar a conexão com o banco de dados
 */
public class Database {

    private static HikariDataSource dataSource;
    private static boolean initialized = false;

    /**
     * Inicializa o pool de conexões com a database
     */
    public static void initialize() {
        if (initialized) {
            BotLogger.warn("Database já inicializado.");
            return;
        }

        try {
            // obtém configurações
            String host = BotConfig.getDatabaseHost();
            String port = BotConfig.getDatabasePort();
            String database = BotConfig.getDatabaseName();
            String user = BotConfig.getDatabaseUser();
            String password = BotConfig.getDatabasePassword();

            BotLogger.info("Configurando conexão com PostgreSQL...");
            BotLogger.debug("Host: " + host + ":" + port);
            BotLogger.debug("Database: " + database);
            BotLogger.debug("User: " + user);

            // configura HikariCP
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(String.format("jdbc:postgresql://%s:%s/%s", host, port, database));
            config.setUsername(user);
            config.setPassword(password);

            // configurações de Pool
            config.setMaximumPoolSize(10); // Máximo de 10 conexões
            config.setMinimumIdle(2);      // Mínimo de 2 conexões idle
            config.setConnectionTimeout(30000); // 30 segundos timeout
            config.setIdleTimeout(600000);      // 10 minutos idle timeout
            config.setMaxLifetime(1800000);     // 30 minutos max lifetime

            // configurações do PostgreSQL
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            // pool name para identificação
            config.setPoolName("DiscordBotPool");

            // cria DataSource
            dataSource = new HikariDataSource(config);

            // Testa conexão
            if (testConnection()) {
                initialized = true;
                BotLogger.success("✅ Conexão com PostgreSQL estabelecida com sucesso!");
                BotLogger.info("Pool de conexões: " + config.getPoolName());
            } else {
                throw new SQLException("Falha no teste de conexão");
            }

        } catch (Exception e) {
            BotLogger.error("❌ Falha crítica ao inicializar banco de dados PostgreSQL", e);
            throw new RuntimeException("Não foi possível conectar ao PostgreSQL", e);
        }
    }

    /**
     * Obtém uma conexão do pool
     */
    public static Connection getConnection() throws SQLException {
        if (!initialized) {
            throw new IllegalStateException("Database não foi inicializado! Chame Database.initialize() primeiro.");
        }

        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Pool de conexões está fechado!");
        }

        return dataSource.getConnection();
    }

    /**
     * Testa a conexão com o banco de dados
     */
    public static boolean testConnection() {
        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn != null && conn.isValid(5);
            if (valid) {
                BotLogger.debug("✓ Teste de conexão bem-sucedido");
            }
            return valid;
        } catch (SQLException e) {
            BotLogger.error("✗ Teste de conexão falhou", e);
            return false;
        }
    }

    /**
     * Retorna estatísticas do pool de conexões
     */
    public static String getPoolStats() {
        if (dataSource == null) {
            return "❌ Pool não inicializado";
        }

        return String.format(
                "📊 Pool Stats: %d ativas | %d idle | %d total | %d aguardando",
                dataSource.getHikariPoolMXBean().getActiveConnections(),
                dataSource.getHikariPoolMXBean().getIdleConnections(),
                dataSource.getHikariPoolMXBean().getTotalConnections(),
                dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
        );
    }

    /**
     * Executa query de teste e retorna estatísticas
     */
    public static String getHealthCheck() {
        try (Connection conn = getConnection()) {
            var stmt = conn.createStatement();
            var rs = stmt.executeQuery("SELECT COUNT(*) as count FROM warns");

            if (rs.next()) {
                int warnCount = rs.getInt("count");
                return String.format("✅ PostgreSQL OK - %d warns registrados | %s",
                        warnCount,
                        getPoolStats()
                );
            }
            return "✅ PostgreSQL OK | " + getPoolStats();

        } catch (SQLException e) {
            BotLogger.error("Health check falhou", e);
            return "❌ PostgreSQL ERROR: " + e.getMessage();
        }
    }

    /**
     * Fecha o pool de conexões
     */
    public static void shutdown() {
        BotLogger.info("Encerrando pool de conexões...");

        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            BotLogger.success("Pool de conexões fechado");
        }

        initialized = false;
    }

    /**
     * Verifica se o database está inicializado
     */
    public static boolean isInitialized() {
        return initialized;
    }
}