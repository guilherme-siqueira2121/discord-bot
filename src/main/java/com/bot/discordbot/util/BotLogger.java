package com.bot.discordbot.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Sistema de logging centralizado para o bot.
 * Registra mensagens em console e arquivo.
 */
public class BotLogger {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter FILE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final Path LOG_DIR = Paths.get("logs");
    private static boolean fileLoggingEnabled = true;

    static {
        try {
            if (!Files.exists(LOG_DIR)) {
                Files.createDirectories(LOG_DIR);
            }
        } catch (IOException e) {
            System.err.println("⚠️ Não foi possível criar diretório de logs: " + e.getMessage());
            fileLoggingEnabled = false;
        }
    }

    public enum Level {
        INFO("ℹ️", "INFO"),
        SUCCESS("✅", "SUCCESS"),
        WARNING("⚠️", "WARN"),
        ERROR("❌", "ERROR"),
        DEBUG("🔍", "DEBUG");

        private final String emoji;
        private final String label;

        Level(String emoji, String label) {
            this.emoji = emoji;
            this.label = label;
        }
    }

    /**
     * Registra mensagem de informação
     */
    public static void info(String message) {
        log(Level.INFO, message);
    }

    /**
     * Registra mensagem de sucesso
     */
    public static void success(String message) {
        log(Level.SUCCESS, message);
    }

    /**
     * Registra mensagem de aviso
     */
    public static void warn(String message) {
        log(Level.WARNING, message);
    }

    /**
     * Registra mensagem de erro
     */
    public static void error(String message) {
        log(Level.ERROR, message);
    }

    /**
     * Registra mensagem de erro com exceção
     */
    public static void error(String message, Throwable throwable) {
        log(Level.ERROR, message + " - " + throwable.getMessage());
        throwable.printStackTrace();
    }

    /**
     * Registra mensagem de debug
     */
    public static void debug(String message) {
        if (isDebugEnabled()) {
            log(Level.DEBUG, message);
        }
    }

    /**
     * Log principal
     */
    private static void log(Level level, String message) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String formattedMessage = String.format("[%s] %s %s: %s",
                timestamp,
                level.emoji,
                level.label,
                message
        );

        // console
        System.out.println(formattedMessage);

        // arquivo
        if (fileLoggingEnabled) {
            writeToFile(timestamp, level, message);
        }
    }

    /**
     * Escreve log em arquivo
     */
    private static void writeToFile(String timestamp, Level level, String message) {
        try {
            String date = LocalDateTime.now().format(FILE_DATE_FORMAT);
            Path logFile = LOG_DIR.resolve("bot-" + date + ".log");

            String logLine = String.format("[%s] [%s] %s%n", timestamp, level.label, message);

            Files.writeString(logFile, logLine,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            System.err.println("⚠️ Erro ao escrever log em arquivo: " + e.getMessage());
        }
    }

    /**
     * Verifica se modo debug está ativo
     */
    private static boolean isDebugEnabled() {
        // verifica a variável de ambiente primeiro
        String debugEnv = System.getenv("DEBUG");
        if ("true".equalsIgnoreCase(debugEnv) || "1".equals(debugEnv)) {
            return true;
        }

        // verifica system property, pois poderia ser setada via config
        String debugProp = System.getProperty("debug.enabled");
        if ("true".equalsIgnoreCase(debugProp)) {
            return true;
        }

        return false;
    }

    /**
     * Log de comando executado
     */
    public static void commandExecuted(String commandName, String userId, String username) {
        info(String.format("Comando /%s executado por %s (ID: %s)",
                commandName, username, userId));
    }

    /**
     * Log de erro de comando
     */
    public static void commandError(String commandName, String userId, String error) {
        error(String.format("Erro no comando /%s (Usuário: %s): %s",
                commandName, userId, error));
    }

    /**
     * Log de ação de moderação
     */
    public static void moderation(String action, String targetId, String moderatorId, String reason) {
        info(String.format("Moderação: %s aplicado em %s por %s - Motivo: %s",
                action, targetId, moderatorId, reason));
    }

    /**
     * Log de database
     */
    public static void database(String operation, boolean success) {
        if (success) {
            debug("Database: " + operation + " - Sucesso");
        } else {
            error("Database: " + operation + " - Falha");
        }
    }
}