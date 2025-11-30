package com.bot.discordbot.moderation.warn.commands;

import com.bot.discordbot.moderation.warn.dao.WarnDAO;
import com.bot.discordbot.moderation.warn.model.Warn;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.time.Instant;
import java.util.List;

public class WarnStatusCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("warnstatus")) return;

        System.out.println("[DEBUG] /warnstatus executado por: " + event.getUser().getAsTag());

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

        // buscar warns ativos
        List<Warn> active = WarnDAO.getActiveWarns(target.getId());

        if (active.isEmpty()) {
            String msg = requester.getId().equals(target.getId())
                    ? "⭐ Você não possui warns ativos."
                    : "⭐ Este usuário não possui warns ativos.";

            event.reply(msg).setEphemeral(true).queue();
            return;
        }

        // montar resposta
        StringBuilder sb = new StringBuilder();
        sb.append("⚠️ **Warns ativos de ").append(target.getEffectiveName()).append("** (").append(active.size()).append(")\n\n");

        for (Warn w : active) {
            sb.append(" | por: ").append(w.getModeratorId() == null ? "Sistema" : "<@" + w.getModeratorId() + ">")
                    .append(" | motivo: `").append(w.getReason()).append("`")
                    .append(" | expira: ").append(Instant.ofEpochMilli(w.getExpiresAt()))
                    .append("\n");
        }

        event.reply(sb.toString()).setEphemeral(true).queue();
    }
}
