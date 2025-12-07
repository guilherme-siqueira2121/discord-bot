package com.bot.discordbot;

import com.bot.discordbot.config.BotConfig;
import com.bot.discordbot.database.Database;
import com.bot.discordbot.database.DatabaseSetup;
import com.bot.discordbot.moderation.warn.commands.WarnClearCommand;
import com.bot.discordbot.moderation.warn.commands.WarnCommand;
import com.bot.discordbot.moderation.warn.commands.WarnStatusCommand;
import com.bot.discordbot.util.BotLogger;
import com.bot.discordbot.commands.PingCommand;
import com.bot.discordbot.commands.NukarCommand;
import com.bot.discordbot.commands.DebugCommand;
import com.bot.discordbot.commands.SetupCommand;
import com.bot.discordbot.listeners.WelcomeAndGoodbye;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;

/**
 * Classe principal do Discord Bot
 */
public class Main {

    private static JDA jda;

    public static void main(String[] args) {
        try {
            // Banner
            printBanner();

            // 1. Inicializa as configurações
            BotLogger.info("Inicializando configurações...");
            BotConfig.initialize();
            BotLogger.info(BotConfig.getConfigSummary());

            // 2. Inicializa o banco de dados
            BotLogger.info("Inicializando banco de dados...");
            Database.initialize();

            // 3. Criar tabelas e índices
            BotLogger.info("Criando/verificando tabelas...");
            DatabaseSetup.initialize();

            // 4. Verificar integridade
            if (!DatabaseSetup.verifyDatabase()) {
                throw new RuntimeException("Falha na verificação do banco de dados!");
            }

            BotLogger.info(Database.getHealthCheck());

            // 3. Construir JDA
            BotLogger.info("Iniciando conexão com Discord...");
            jda = buildJDA();
            jda.awaitReady();

            // 4. Registra comandos na guild
            BotLogger.info("Registrando comandos slash...");
            registerCommands();

            // 5. Shutdown hook
            registerShutdownHook();

            BotLogger.success("🤖 Bot está online e operacional!");
            BotLogger.info("Pressione Ctrl+C para desligar");

        } catch (Exception e) {
            BotLogger.error("❌ Falha crítica ao iniciar o bot", e);
            System.exit(1);
        }
    }

    /**
     * Constrói e configura o JDA
     */
    private static JDA buildJDA() throws InterruptedException {
        String token = BotConfig.getBotToken();

        return JDABuilder.createDefault(token)
                // Gateway Intents
                .enableIntents(
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.GUILD_MODERATION,
                        GatewayIntent.GUILD_MESSAGE_REACTIONS
                )
                // Event Listeners
                .addEventListeners(
                        new PingCommand(),
                        new NukarCommand(),
                        new DebugCommand(),
                        new SetupCommand(),
                        new WelcomeAndGoodbye(),
                        new WarnStatusCommand(),
                        new WarnCommand(),
                        new WarnClearCommand()
                )
                .build();
    }

    /**
     * Registra comandos slash na guild
     */
    private static void registerCommands() {
        String guildId = BotConfig.getGuildId();
        var guild = jda.getGuildById(guildId);

        if (guild == null) {
            BotLogger.error("❌ Guild não encontrada: " + guildId);
            throw new IllegalStateException("Guild configurada não existe");
        }

        guild.updateCommands()
                .addCommands(
                        // comando Ping
                        Commands.slash("ping", "Responde com Pong!"),

                        // comando Setup
                        Commands.slash("setup", "Envia mensagens permanentes (info, regras)")
                                .addOption(OptionType.STRING, "tipo", "Tipo de mensagem: info ou regras", true)
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS)),

                        // comando Debug
                        Commands.slash("debug", "Mostra informações de debug do sistema (admin only)")
                                .addOption(OptionType.STRING, "action", "Ação a executar", false)
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS)),

                        // comando Nukar
                        Commands.slash("nukar", "Apaga até 1000 mensagens do canal atual")
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),

                        // comando WarnStatus
                        Commands.slash("warnstatus", "Mostra seus warns (staff pode ver de outros)")
                                .addOption(OptionType.USER, "user", "Usuário a consultar (somente staff)", false),

                        // comando Warn
                        Commands.slash("warn", "Aplica um warn a um usuário")
                                .addOption(OptionType.USER, "user", "Usuário a ser advertido", true)
                                .addOption(OptionType.STRING, "motivo", "Motivo do warn", true)
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS)),

                        // comando WarnClear
                        Commands.slash("warnclear", "Remove todos os warns de um usuário")
                                .addOption(OptionType.USER, "user", "Usuário", true)
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS))
                )
                .queue(
                        success -> BotLogger.success("Comandos registrados com sucesso!"),
                        error -> BotLogger.error("Erro ao registrar comandos", error)
                );
    }

    /**
     * Registra hook para shutdown
     */
    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            BotLogger.info("Desligando bot...");

            if (jda != null) {
                jda.shutdown();
                BotLogger.info("JDA encerrado");
            }

            Database.shutdown();
            BotLogger.success("Bot desligado com sucesso!");
        }, "ShutdownHook"));
    }

    /**
     * banner
     */
    private static void printBanner() {
        String banner = """
            ╔═══════════════════════════════════════╗
            ║                                       ║
            ║        GEHIRN DISCORD BOT             ║
            ║        Sistema de Moderação           ║
            ║                                       ║
            ╚═══════════════════════════════════════╝
            """;
        System.out.println(banner);
    }

    public static JDA getJDA() {
        return jda;
    }
}