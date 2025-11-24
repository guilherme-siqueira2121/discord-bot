package com.bot.discordbot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class NukarCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("nukar")) return;

        event.reply("Nukando 1000 mensagens...").queue();

        event.getChannel().getHistory().retrievePast(100).queue(messages -> {
            event.getChannel().purgeMessages(messages);
        });

        for (int i = 0; i < 10; i++) {
            event.getChannel().getHistory().retrievePast(100).queue(messages -> {
                event.getChannel().purgeMessages(messages);
            });
        }
    }
}
