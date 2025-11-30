package com.bot.discordbot.moderation.warn.commands;

import com.bot.discordbot.moderation.warn.service.WarnService;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class WarnCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("warn")) return;

        Member moderator = event.getMember();
        var optUser = event.getOption("user");
        var optReason = event.getOption("motivo");

        if (moderator == null || !moderator.hasPermission(Permission.MODERATE_MEMBERS)) {
            event.reply("❌ Você não tem permissão para usar este comando.").setEphemeral(true).queue();
            return;
        }

        if (optUser == null || optReason == null) {
            event.reply("Uso: /warn user: @user motivo: texto").setEphemeral(true).queue();
            return;
        }

        Member target = optUser.getAsMember();
        String reason = optReason.getAsString();

        if (target == null) {
            event.reply("Usuário inválido.").setEphemeral(true).queue();
            return;
        }

        // salva e aplica
        WarnService.addWarn(target.getId(), moderator.getId(), reason, event.getGuild());

        event.reply("⚠️ Warn aplicado a " + target.getAsMention() + " (`" + reason + "`)").queue();
    }
}
