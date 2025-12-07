package com.bot.discordbot.commands;

import com.bot.discordbot.database.Database;
import com.bot.discordbot.database.DatabaseSetup;
import com.bot.discordbot.moderation.warn.dao.WarnDAO;
import com.bot.discordbot.util.BotLogger;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Comando de debug para verificar o estado do banco de dados.
 */
public class DebugCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("debug")) return;

        // somente staff pode usar
        if (event.getMember() == null || !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("❌ Apenas administradores podem usar este comando.").setEphemeral(true).queue();
            return;
        }

        var action = event.getOption("action");
        String actionValue = action != null ? action.getAsString() : "status";

        event.deferReply(true).queue();

        switch (actionValue) {
            case "status" -> showStatus(event);
            case "reset" -> resetDatabase(event);
            case "verify" -> verifyDatabase(event);
            default -> event.getHook().editOriginal("❌ Ação desconhecida: " + actionValue).queue();
        }
    }

    private void showStatus(SlashCommandInteractionEvent event) {
        try {
            StringBuilder response = new StringBuilder();
            response.append("🔍 **Debug do Sistema**\n\n");

            // 1. Health check do database
            response.append("**Database:**\n");
            response.append(Database.getHealthCheck()).append("\n\n");

            // 2. Estatísticas gerais
            response.append("**Estatísticas:**\n");
            response.append(WarnDAO.getStatistics()).append("\n\n");

            // 3. Testar cache de membros
            response.append("**Teste de Cache:**\n");
            if (event.getGuild() != null) {
                long cached = event.getGuild().getMemberCache().size();
                response.append("Membros em cache: ").append(cached).append("\n");
                response.append("Total de membros: ").append(event.getGuild().getMemberCount()).append("\n\n");
            }

            // 4. Listar todos os warns (últimos 10)
            response.append("**Últimos Warns no Banco:**\n");
            try (Connection conn = Database.getConnection();
                 Statement stmt = conn.createStatement()) {

                ResultSet rs = stmt.executeQuery(
                        "SELECT id, user_id, moderator_id, reason, timestamp, expires_at " +
                                "FROM warns ORDER BY timestamp DESC LIMIT 10"
                );

                int count = 0;
                long now = System.currentTimeMillis();

                while (rs.next()) {
                    count++;
                    int id = rs.getInt("id");
                    String userId = rs.getString("user_id");
                    long expiresAt = rs.getLong("expires_at");
                    boolean isActive = expiresAt > now;

                    response.append(String.format(
                            "%d. ID=%d | User=%s | Expira=<t:%d:R> | %s\n",
                            count,
                            id,
                            maskUserId(userId),
                            expiresAt / 1000,
                            isActive ? "✅ ATIVO" : "❌ EXPIRADO"
                    ));
                }

                if (count == 0) {
                    response.append("_(Nenhum warn no banco)_\n");
                }

            } catch (Exception e) {
                response.append("❌ Erro ao consultar warns: ").append(e.getMessage()).append("\n");
                BotLogger.error("Erro no comando debug", e);
            }

            // 5. informações do usuário que executou
            response.append("\n**Seu UserID:** `").append(event.getUser().getId()).append("`\n");

            int yourWarns = WarnDAO.countActiveWarns(event.getUser().getId());
            response.append("**Seus warns ativos:** ").append(yourWarns).append("\n");

            response.append("\n_Use `/debug action:reset` para resetar o banco (APAGA TUDO!)_");
            response.append("\n_Use `/debug action:verify` para verificar integridade_");

            event.getHook().editOriginal(response.toString()).queue();

        } catch (Exception e) {
            BotLogger.error("Erro crítico no comando debug", e);
            event.getHook().editOriginal("❌ Erro ao executar debug: " + e.getMessage()).queue();
        }
    }

    private void resetDatabase(SlashCommandInteractionEvent event) {
        try {
            BotLogger.warn("Reset de banco solicitado por: " + event.getUser().getAsTag());

            DatabaseSetup.resetDatabase();

            event.getHook().editOriginal(
                    "✅ **Banco de dados resetado com sucesso!**\n\n" +
                            "Todas as tabelas foram recriadas.\n" +
                            "Todos os dados foram perdidos.\n\n" +
                            "Execute `/debug action:verify` para confirmar."
            ).queue();

        } catch (Exception e) {
            BotLogger.error("Erro ao resetar banco", e);
            event.getHook().editOriginal("❌ Erro ao resetar banco: " + e.getMessage()).queue();
        }
    }

    private void verifyDatabase(SlashCommandInteractionEvent event) {
        try {
            boolean valid = DatabaseSetup.verifyDatabase();

            if (valid) {
                event.getHook().editOriginal(
                        "✅ **Banco de dados está OK!**\n\n" +
                                "Todas as tabelas necessárias estão presentes.\n" +
                                Database.getHealthCheck()
                ).queue();
            } else {
                event.getHook().editOriginal(
                        "❌ **Problema detectado no banco de dados!**\n\n" +
                                "Algumas tabelas estão faltando.\n" +
                                "Execute `/debug action:reset` para recriar."
                ).queue();
            }

        } catch (Exception e) {
            BotLogger.error("Erro ao verificar banco", e);
            event.getHook().editOriginal("❌ Erro ao verificar banco: " + e.getMessage()).queue();
        }
    }

    /**
     * Mascara UserID para privcidade nos logs
     */
    private String maskUserId(String userId) {
        if (userId == null || userId.length() < 4) {
            return "****";
        }
        return "****" + userId.substring(userId.length() - 4);
    }
}