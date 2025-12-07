package com.bot.discordbot.commands;

import com.bot.discordbot.config.ServerMessages;
import com.bot.discordbot.util.BotLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.Color;

/**
 * Comando para configurar mensagens permanentes no servidor
 * (Boas-vindas, Regras, etc)
 */
public class SetupCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("setup")) return;

        // apenas administradores podem usar
        if (event.getMember() == null || !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("❌ Apenas administradores podem usar este comando.").setEphemeral(true).queue();
            return;
        }

        var typeOption = event.getOption("tipo");
        if (typeOption == null) {
            event.reply("❌ Especifique o tipo: `info` ou `regras`").setEphemeral(true).queue();
            return;
        }

        String type = typeOption.getAsString();

        event.deferReply(true).queue();

        switch (type.toLowerCase()) {
            case "info" -> sendServerInfo(event);
            case "regras" -> sendServerRules(event);
            default -> event.getHook().editOriginal("❌ Tipo inválido. Use `info` ou `regras`").queue();
        }
    }

    /**
     * Envia mensagem de informações do servidor
     */
    private void sendServerInfo(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null || event.getChannel() == null) {
            event.getHook().editOriginal("❌ Erro ao obter canal.").queue();
            return;
        }

        TextChannel channel = event.getChannel().asTextChannel();

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(ServerMessages.INFO_TITLE)
                .setDescription(ServerMessages.INFO_DESCRIPTION)
                .setColor(new Color(88, 101, 242)) // Cor do Discord (Blurple)
                .setThumbnail(event.getGuild().getIconUrl())
                .addField(ServerMessages.InfoFields.CHAT_TITLE, ServerMessages.InfoFields.CHAT_VALUE, false)
                .addField(ServerMessages.InfoFields.GAMES_TITLE, ServerMessages.InfoFields.GAMES_VALUE, false)
                .addField(ServerMessages.InfoFields.CREATIVE_TITLE, ServerMessages.InfoFields.CREATIVE_VALUE, false)
                .addField(ServerMessages.InfoFields.SUPPORT_TITLE, ServerMessages.InfoFields.SUPPORT_VALUE, false)
                .addField("", "━━━━━━━━━━━━━━━━━━━━━", false)
                .addField(ServerMessages.InfoFields.IMPORTANT_TITLE, ServerMessages.InfoFields.IMPORTANT_VALUE, false)
                .addField(ServerMessages.InfoFields.FINAL_TITLE, ServerMessages.InfoFields.FINAL_VALUE, false)
                .setFooter("Gehirn • Romdo", event.getGuild().getIconUrl())
                .setTimestamp(java.time.Instant.now());

        channel.sendMessageEmbeds(embed.build()).queue(
                success -> {
                    BotLogger.success("Mensagem de info enviada no canal: " + channel.getName());
                    event.getHook().editOriginal(
                            "✅ **Mensagem de informações enviada com sucesso!**\n" +
                                    "Canal: " + channel.getAsMention()
                    ).queue();
                },
                error -> {
                    BotLogger.error("Erro ao enviar mensagem de info", error);
                    event.getHook().editOriginal("❌ Erro ao enviar mensagem: " + error.getMessage()).queue();
                }
        );
    }

    /**
     * Envia mensagem de regras do servidor
     */
    private void sendServerRules(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null || event.getChannel() == null) {
            event.getHook().editOriginal("❌ Erro ao obter canal.").queue();
            return;
        }

        TextChannel channel = event.getChannel().asTextChannel();

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(ServerMessages.RULES_TITLE)
                .setDescription(ServerMessages.RULES_DESCRIPTION)
                .setColor(Color.RED)
                .setThumbnail(event.getGuild().getIconUrl())
                .addField(ServerMessages.Rules.RULE1_TITLE, ServerMessages.Rules.RULE1_VALUE, false)
                .addField(ServerMessages.Rules.RULE2_TITLE, ServerMessages.Rules.RULE2_VALUE, false)
                .addField(ServerMessages.Rules.RULE3_TITLE, ServerMessages.Rules.RULE3_VALUE, false)
                .addField(ServerMessages.Rules.RULE4_TITLE, ServerMessages.Rules.RULE4_VALUE, false)
                .addField(ServerMessages.Rules.RULE5_TITLE, ServerMessages.Rules.RULE5_VALUE, false)
                .addField(ServerMessages.Rules.RULE6_TITLE, ServerMessages.Rules.RULE6_VALUE, false)
                .addField(ServerMessages.Rules.RULE7_TITLE, ServerMessages.Rules.RULE7_VALUE, false)
                .addField(ServerMessages.Rules.RULE8_TITLE, ServerMessages.Rules.RULE8_VALUE, false)
                .addField("", "━━━━━━━━━━━━━━━━━━━━━", false)
                .addField(ServerMessages.Rules.PUNISHMENT_TITLE, ServerMessages.Rules.PUNISHMENT_VALUE, false)
                .addField(ServerMessages.Rules.STAFF_TITLE, ServerMessages.Rules.STAFF_VALUE, false)
                .addField(ServerMessages.Rules.FINAL_NOTE_TITLE, ServerMessages.Rules.FINAL_NOTE_VALUE, false)
                .setFooter(ServerMessages.RULES_FOOTER, event.getGuild().getIconUrl())
                .setTimestamp(java.time.Instant.now());

        channel.sendMessageEmbeds(embed.build()).queue(
                success -> {
                    BotLogger.success("Mensagem de regras enviada no canal: " + channel.getName());
                    event.getHook().editOriginal(
                            "✅ **Mensagem de regras enviada com sucesso!**\n" +
                                    "Canal: " + channel.getAsMention()
                    ).queue();
                },
                error -> {
                    BotLogger.error("Erro ao enviar mensagem de regras", error);
                    event.getHook().editOriginal("❌ Erro ao enviar mensagem: " + error.getMessage()).queue();
                }
        );
    }
}