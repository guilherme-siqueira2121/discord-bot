package com.bot.discordbot.listeners;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;

public class WelcomeAndGoodbye extends ListenerAdapter {

    private final String welcomeChannelId = "1442909201642033212";
    private final String exitChannelId = "1442909243236946031";
    private final String autoRoleId = "1442495578847318057";

    // entrada de membro
    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {

        var guild = event.getGuild();
        var member = event.getMember();

        // atribuir cargo automático
        var role = guild.getRoleById(autoRoleId);
        if (role != null) {
            guild.addRoleToMember(member, role).queue(
                    success -> System.out.println("✔ Behirn, o fodão atribuiu cargo a " + member.getEffectiveName()),
                    error -> System.out.println("❌ Erro ao atribuir cargo: " + error.getMessage())
            );
        }

        // logar entrada
        var channel = guild.getTextChannelById(welcomeChannelId);
        if (channel == null) return;

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("👋 Novo membro entrou!")
                .setDescription("Gehirn, o fodão te deseja boas-vindas, " + member.getAsMention() + "!")
                .setColor(Color.GREEN)
                .setThumbnail(member.getEffectiveAvatarUrl())
                .setFooter("ID: " + member.getId());

        channel.sendMessageEmbeds(embed.build()).queue();
    }

    // saída de membro
    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {

        var guild = event.getGuild();
        var user = event.getUser();

        var channel = guild.getChannelById(
                net.dv8tion.jda.api.entities.channel.concrete.TextChannel.class,
                exitChannelId
        );

        if (channel == null) return;

        // verifica se o usuário está na lista de banidos
        guild.retrieveBan(user).queue(
                ban -> {
                    // se achou = usuário banido
                    EmbedBuilder embed = new EmbedBuilder()
                            .setTitle("🔨 Gehirn, o fodão baniu alguém.")
                            .setDescription("O usuário **" + user.getName() + "** foi banido do servidor por ser um pascácio.")
                            .setThumbnail(user.getAvatarUrl())
                            .setColor(Color.RED);

                    channel.sendMessageEmbeds(embed.build()).queue();
                },
                err -> {
                    // se não = sáida comum
                    EmbedBuilder embed = new EmbedBuilder()
                            .setTitle("🚪 Membro saiu do servidor")
                            .setDescription("O usuário **" + user.getName() + "** saiu do servidor por não gostar de Gehirn, o fodão... Mas Gehirn continua sendo fodão!")
                            .setThumbnail(user.getAvatarUrl())
                            .setColor(Color.ORANGE);

                    channel.sendMessageEmbeds(embed.build()).queue();
                }
        );
    }
}
