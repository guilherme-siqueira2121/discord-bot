package com.bot.discordbot.listeners;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class BadWordsFilter extends ListenerAdapter {

    private static final Set<String> BLOCKED_WORDS = new HashSet<>(Arrays.asList(
            "arrombado",
            "arrombada",
            "filho da puta",
            "filha da puta",
            "negro",
            "vai tomar no cu",
            "vtmnc",
            "pau no cu",
            "pnc",
            "pica",
            "rola",
            "buceta",
            "discord.gg/"
    ));

    private final String logChannelId = "1442976990822006975";

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        if (event.getAuthor().isBot()) return;

        String content = event.getMessage().getContentRaw().toLowerCase();

        for (String bad : BLOCKED_WORDS) {
            if (content.contains(bad)) {

                var member = event.getMember();
                if (member == null) return;

                event.getMessage().delete().queue();

                WarnManager.addWarn(member.getId());
                int warns = WarnManager.getWarns(member.getId());

                event.getChannel().sendMessage(
                        "⚠️ **" + member.getEffectiveName() + "**, Gehirn removeu um mensagem. Advertência **" + warns + "/7**."
                ).queue();

                sendLog(event, member, content, bad, warns);

                applyPunishment(event, member, warns);

                break;
            }
        }
    }

    private void sendLog(MessageReceivedEvent event, net.dv8tion.jda.api.entities.Member member,
                         String msg, String palavra, int warns) {

        var logChannel = event.getJDA().getTextChannelById(logChannelId);
        if (logChannel == null) return;

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🛑 Filtro de Palavrões")
                .setColor(Color.RED)
                .addField("Usuário", member.getAsMention(), false)
                .addField("Mensagem", msg, false)
                .addField("Palavra detectada", palavra, false)
                .addField("Advertências", warns + "/7", false);

        logChannel.sendMessageEmbeds(embed.build()).queue();
    }

    private void applyPunishment(MessageReceivedEvent event, net.dv8tion.jda.api.entities.Member member, int warns) {

        switch (warns) {

            // warn de aviso
            case 1 -> event.getChannel().sendMessage("⚠️ 1/7 Warn para " + member.getAsMention()).queue();

            case 3 -> {
                // muta o usuário por 10 minutos
                member.timeoutFor(java.time.Duration.ofMinutes(10)).queue(
                        v -> event.getChannel().sendMessage("🔇 " + member.getAsMention() + " foi mutado por 10 minutos (3 warns).").queue(),
                        err -> System.out.println("Erro ao mutar: " + err.getMessage())
                );
            }

            case 5 -> {
                // kicka o usuário
                if (member.hasPermission(Permission.KICK_MEMBERS)) return;
                member.kick()
                        .reason("Excedeu 5 warns automáticos")
                        .queue(
                                v -> event.getChannel().sendMessage("👢 " + member.getAsMention() + " foi expulso (5 warns).").queue(),
                                err -> System.out.println("Erro ao kickar: " + err.getMessage())
                        );
            }

            case 7 -> {
                // bane
                member.ban(1, java.util.concurrent.TimeUnit.DAYS) // apaga 1 dia de mensagens
                        .reason("Excedeu 7 warns (ban automático)")
                        .queue(
                                v -> event.getChannel().sendMessage("🔨 " + member.getAsMention() + " foi **banido** por comportamento ofensivo.").queue(),
                                err -> System.out.println("Erro ao banir: " + err.getMessage())
                        );

            }
        }
    }
}
