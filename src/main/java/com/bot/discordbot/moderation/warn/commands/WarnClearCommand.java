package com.bot.discordbot.moderation.warn.commands;

import com.bot.discordbot.moderation.warn.dao.WarnDAO;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class WarnClearCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("warnclear")) return;

        var optUser = event.getOption("user");
        if (optUser == null) {
            event.reply("Uso: /warnclear user:@user").setEphemeral(true).queue();
            return;
        }

        if (event.getMember() == null || !event.getMember().hasPermission(Permission.MODERATE_MEMBERS)) {
            event.reply("Você não tem permissão.").setEphemeral(true).queue();
            return;
        }

        String targetId = optUser.getAsUser().getId();
        WarnDAO.clearUserWarns(targetId);
        event.reply("✅ Warns do usuário foram removidos.").queue();
    }
}
