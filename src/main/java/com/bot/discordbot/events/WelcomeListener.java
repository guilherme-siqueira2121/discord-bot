package com.bot.discordbot.events;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;

public class WelcomeListener extends ListenerAdapter {

    private final String welcomeChannelId = "1442537943020474388";
    private final String autoRoleId = "1442495578847318057";

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {

        var guild = event.getGuild();
        var member = event.getMember();

        var role = guild.getRoleById(autoRoleId);
        if (role != null) {
            guild.addRoleToMember(member, role).queue(
                    sucess -> System.out.println("✔ Cargo atribuído a " + member.getEffectiveName()),
                    error -> System.out.println("❌ Erro ao atribuir cargo: " + error.getMessage())
            );
        }

        var channel = guild.getTextChannelById(welcomeChannelId);
        if (channel == null) return;
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("👋 Gehirn te deseja boas-vindas ao servidor!");
        embed.setDescription("Seja muito bem-vindo(a), **" + member.getAsMention() + "**!\n\n"
                + "Sinta-se à vontade para conversar, interagir e aproveitar o servidor.");
        embed.setColor(Color.CYAN);
        embed.setThumbnail(member.getEffectiveAvatarUrl());
        embed.setFooter("Gehirn fica feliz em ter você aqui! 🙂");

        channel.sendMessageEmbeds(embed.build()).queue();
    }
}
