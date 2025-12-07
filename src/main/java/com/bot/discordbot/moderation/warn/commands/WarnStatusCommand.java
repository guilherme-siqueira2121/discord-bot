package com.bot.discordbot.moderation.warn.commands;

import com.bot.discordbot.moderation.warn.dao.WarnDAO;
import com.bot.discordbot.moderation.warn.model.Warn;
import com.bot.discordbot.util.BotLogger;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;

public class WarnStatusCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("warnstatus")) return;

        BotLogger.debug("=== INÍCIO warnstatus ===");
        BotLogger.commandExecuted("warnstatus", event.getUser().getId(), event.getUser().getName());

        // funciona em guild
        if (event.getGuild() == null) {
            event.reply("Este comando só pode ser usado em um servidor.").setEphemeral(true).queue();
            return;
        }

        Member requester = event.getMember();
        if (requester == null) {
            event.reply("Erro interno: requester null.").setEphemeral(true).queue();
            return;
        }

        // se o usuário forneceu um target, só staff pode visualizá-lo
        var optUser = event.getOption("user");
        Member target;

        boolean isStaff = requester.hasPermission(Permission.BAN_MEMBERS);

        if (optUser != null) {
            target = optUser.getAsMember();

            if (target == null) {
                event.reply("Usuário especificado não está no servidor.").setEphemeral(true).queue();
                return;
            }

            if (!isStaff) {
                event.reply("❌ Apenas a equipe pode ver os warns de outros membros.").setEphemeral(true).queue();
                return;
            }
        } else {
            // se não informou usuário, mostra os próprios warns
            target = requester;
        }

        String targetUserId = target.getId();
        BotLogger.debug("Consultando warns para userId: " + targetUserId);

        // Debug: contar warns primeiro
        int count = WarnDAO.countActiveWarns(targetUserId);
        BotLogger.debug("countActiveWarns retornou: " + count);

        // buscar warns ativos
        List<Warn> active = WarnDAO.getActiveWarns(targetUserId);
        BotLogger.debug("getActiveWarns retornou: " + active.size() + " warns");

        // Debug adicional: mostrar cada warn
        for (int i = 0; i < active.size(); i++) {
            Warn w = active.get(i);
            BotLogger.debug(String.format("Warn %d: id=%d, expires=%d, now=%d, isActive=%b",
                    i + 1, w.getId(), w.getExpiresAt(), System.currentTimeMillis(), w.isActive()));
        }

        if (active.isEmpty()) {
            String msg = requester.getId().equals(target.getId())
                    ? "⭐ Você não possui warns ativos."
                    : "⭐ " + target.getEffectiveName() + " não possui warns ativos.";

            BotLogger.debug("Nenhum warn ativo encontrado");
            event.reply(msg).setEphemeral(true).queue();
            return;
        }

        // montar resposta
        StringBuilder sb = new StringBuilder();
        sb.append("⚠️ **Warns ativos de ").append(target.getEffectiveName())
                .append("** (").append(active.size()).append(")\n\n");

        for (int i = 0; i < active.size(); i++) {
            Warn w = active.get(i);
            sb.append("**").append(i + 1).append(".** ");
            sb.append("ID: `").append(w.getId()).append("`");
            sb.append(" | Por: ").append(w.getModeratorId() == null ? "Sistema" : "<@" + w.getModeratorId() + ">");
            sb.append("\n   Motivo: `").append(w.getReason()).append("`");
            sb.append("\n   Expira: <t:").append(w.getExpiresAt() / 1000).append(":R>");
            sb.append("\n");
        }

        BotLogger.debug("Enviando resposta com " + active.size() + " warns");
        BotLogger.debug("=== FIM warnstatus ===");

        event.reply(sb.toString()).setEphemeral(true).queue();
    }
}