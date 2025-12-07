package com.bot.discordbot.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Classe responsável por gerenciar todas as configurações do bot.
 * Carrega dados de variáveis de ambiente e arquivo de propriedades.
 */
public class BotConfig {

    private static final Properties properties = new Properties();
    private static boolean initialized = false;

    // ==================== DISCORD ====================
    private static String botToken;
    private static String guildId;

    // ==================== CANAIS ====================
    private static String welcomeChannelId;
    private static String exitChannelId;
    private static String logChannelId;

    // ==================== CARGOS ====================
    private static String autoRoleId;

    // ==================== DATABASE ====================
    private static String databaseHost;
    private static String databasePort;
    private static String databaseName;
    private static String databaseUser;
    private static String databasePassword;

    /**
     * Inicializa as configurações do bot.
     */
    public static void initialize() {
        if (initialized) {
            System.out.println("[Config] Já inicializado.");
            return;
        }

        loadFromPropertiesFile();
        loadFromEnvironmentVariables();
        validateRequiredConfigs();

        initialized = true;
        System.out.println("[Config] ✅ Configurações carregadas com sucesso!");
    }

    /**
     * Carrega configurações do arquivo config.properties (se existir)
     */
    private static void loadFromPropertiesFile() {
        try (InputStream input = new FileInputStream("config.properties")) {
            properties.load(input);
            System.out.println("[Config] Arquivo config.properties carregado.");

            // serve para carregar valores do properties
            botToken = properties.getProperty("discord.bot.token");
            guildId = properties.getProperty("discord.guild.id");
            welcomeChannelId = properties.getProperty("discord.channel.welcome");
            exitChannelId = properties.getProperty("discord.channel.exit");
            logChannelId = properties.getProperty("discord.channel.logs");
            autoRoleId = properties.getProperty("discord.role.auto");

            // Database
            databaseHost = properties.getProperty("database.host", "localhost");
            databasePort = properties.getProperty("database.port", "5432");
            databaseName = properties.getProperty("database.name", "discord_bot");
            databaseUser = properties.getProperty("database.user", "postgres");
            databasePassword = properties.getProperty("database.password", "");

        } catch (IOException e) {
            System.out.println("[Config] ⚠️ Arquivo config.properties não encontrado. Usando apenas variáveis de ambiente.");
        }
    }

    /**
     * Carrega e sobrescreve com variáveis de ambiente (prioritárias)
     */
    private static void loadFromEnvironmentVariables() {
        botToken = getEnvOrDefault("DISCORD_BOT_TOKEN", botToken);
        guildId = getEnvOrDefault("DISCORD_GUILD_ID", guildId);
        welcomeChannelId = getEnvOrDefault("WELCOME_CHANNEL_ID", welcomeChannelId);
        exitChannelId = getEnvOrDefault("EXIT_CHANNEL_ID", exitChannelId);
        logChannelId = getEnvOrDefault("LOG_CHANNEL_ID", logChannelId);
        autoRoleId = getEnvOrDefault("AUTO_ROLE_ID", autoRoleId);

        // Database
        databaseHost = getEnvOrDefault("DB_HOST", databaseHost);
        databasePort = getEnvOrDefault("DB_PORT", databasePort);
        databaseName = getEnvOrDefault("DB_NAME", databaseName);
        databaseUser = getEnvOrDefault("DB_USER", databaseUser);
        databasePassword = getEnvOrDefault("DB_PASSWORD", databasePassword);
    }

    /**
     * Retorna variável de ambiente ou valor padrão
     */
    private static String getEnvOrDefault(String envKey, String defaultValue) {
        String value = System.getenv(envKey);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    /**
     * Valida se as configurações obrigatórias foram definidas
     */
    private static void validateRequiredConfigs() {
        if (botToken == null || botToken.isEmpty()) {
            throw new IllegalStateException("❌ DISCORD_BOT_TOKEN não foi configurado!");
        }
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalStateException("❌ DISCORD_GUILD_ID não foi configurado!");
        }
    }

    // ==================== GETTERS ====================

    public static String getBotToken() {
        ensureInitialized();
        return botToken;
    }

    public static String getGuildId() {
        ensureInitialized();
        return guildId;
    }

    public static String getWelcomeChannelId() {
        ensureInitialized();
        return welcomeChannelId;
    }

    public static String getExitChannelId() {
        ensureInitialized();
        return exitChannelId;
    }

    public static String getLogChannelId() {
        ensureInitialized();
        return logChannelId;
    }

    public static String getAutoRoleId() {
        ensureInitialized();
        return autoRoleId;
    }

    public static String getDatabaseHost() {
        ensureInitialized();
        return databaseHost;
    }

    public static String getDatabasePort() {
        ensureInitialized();
        return databasePort;
    }

    public static String getDatabaseName() {
        ensureInitialized();
        return databaseName;
    }

    public static String getDatabaseUser() {
        ensureInitialized();
        return databaseUser;
    }

    public static String getDatabasePassword() {
        ensureInitialized();
        return databasePassword;
    }

    /**
     * Verifica se o bot foi inicializado
     */
    private static void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("BotConfig não foi inicializado! Chame BotConfig.initialize() primeiro.");
        }
    }

    /**
     * Retorna informações de configuração (sem dados sensíveis)
     */
    public static String getConfigSummary() {
        return String.format("""
            [Config] Resumo das Configurações:
              - Guild ID: %s
              - Welcome Channel: %s
              - Exit Channel: %s
              - Log Channel: %s
              - Auto Role: %s
              - Database: %s@%s:%s/%s
            """,
                maskId(guildId),
                maskId(welcomeChannelId),
                maskId(exitChannelId),
                maskId(logChannelId),
                maskId(autoRoleId),
                databaseUser,
                databaseHost,
                databasePort,
                databaseName
        );
    }

    /**
     * Mascara IDs para logs (mostra apenas últimos 4 dígitos)
     */
    private static String maskId(String id) {
        if (id == null || id.length() < 4) return "****";
        return "****" + id.substring(id.length() - 4);
    }
}