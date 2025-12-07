package com.bot.discordbot.moderation.warn.commands;

import com.bot.discordbot.moderation.warn.service.WarnService;
import com.bot.discordbot.util.BotLogger;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class WarnCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("warn")) return;

        BotLogger.debug("=== INÍCIO ===");

        Member moderator = event.getMember();
        var optUser = event.getOption("user");
        var optReason = event.getOption("motivo");

        // validação de permissões
        if (moderator == null || !moderator.hasPermission(Permission.MODERATE_MEMBERS)) {
            event.reply("❌ Você não tem permissão para usar este comando.").setEphemeral(true).queue();
            return;
        }

        // validação de parâmetros
        if (optUser == null || optReason == null) {
            event.reply("❌ Uso: /warn user:@user motivo:texto").setEphemeral(true).queue();
            return;
        }

        User targetUser = optUser.getAsUser();
        String reason = optReason.getAsString();

        BotLogger.debug("Target User ID: " + targetUser.getId());
        BotLogger.debug("Target User Name: " + targetUser.getName());
        BotLogger.debug("Moderator: " + moderator.getEffectiveName());
        BotLogger.debug("Reason: " + reason);

        //verifica se o usuário está no servidor
        if (event.getGuild() == null) {
            event.reply("❌ Este comando só funciona em servidores.").setEphemeral(true).queue();
            return;
        }

        // tenta buscar o membro
        event.getGuild().retrieveMemberById(targetUser.getId()).queue(
                target -> {
                    BotLogger.debug("Membro encontrado no servidor: " + target.getEffectiveName());

                    if (target.getUser().isBot()) {
                        event.reply("❌ Não é possível aplicar warn em bots.").setEphemeral(true).queue();
                        return;
                    }

                    if (target.hasPermission(Permission.MODERATE_MEMBERS)) {
                        event.reply("❌ Não é possível aplicar warn em membros da staff.").setEphemeral(true).queue();
                        return;
                    }

                    if (target.getId().equals(moderator.getId())) {
                        event.reply("❌ Você não pode aplicar warn em si mesmo.").setEphemeral(true).queue();
                        return;
                    }

                    // aplica o warn
                    BotLogger.info("Aplicando warn a " + target.getEffectiveName() + " (ID: " + target.getId() + ")");

                    boolean success = WarnService.addWarn(
                            target.getId(),  // ID do membro do servidor
                            moderator.getId(),
                            reason,
                            event.getGuild()
                    );

                    BotLogger.debug("Resultado do addWarn: " + success);

                    if (success) {
                        // aguarda um pouco para o banco processar
                        try {
                            Thread.sleep(100); // 100ms
                        } catch (InterruptedException e) {
                            // ignora
                        }

                        // conta warns atuais
                        int warnCount = WarnService.getActiveWarns(target.getId()).size();
                        BotLogger.debug("Warns ativos após inserção: " + warnCount);

                        String punishment = WarnService.getPunishmentDescription(warnCount);

                        event.reply(String.format(
                                "⚠️ **Warn aplicado com sucesso!**\n\n" +
                                        "👤 Usuário: %s\n" +
                                        "📝 Motivo: `%s`\n" +
                                        "📊 Total de warns: **%d/6**\n" +
                                        "⚡ Punição: %s",
                                target.getAsMention(),
                                reason,
                                warnCount,
                                punishment
                        )).queue();

                        BotLogger.success("Warn aplicado e feedback enviado!");
                    } else {
                        event.reply("❌ Erro ao aplicar warn. Verifique os logs.").setEphemeral(true).queue();
                        BotLogger.error("Falha ao aplicar warn - addWarn retornou false");
                    }

                    BotLogger.debug("=== FIM WarnCommand ===");
                },
                error -> {
                    BotLogger.error("Erro ao buscar membro: " + error.getMessage());
                    event.reply("❌ Usuário não encontrado no servidor. Ele pode ter saído.").setEphemeral(true).queue();
                }
        );
    }
}