package com.bot.discordbot.listeners;

import com.bot.discordbot.config.BotConfig;
import com.bot.discordbot.util.BotLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.Color;

/**
 * Listener responsável por mensagens de boas-vindas e despedidas
 * Também atribui cargo automático para novos membros
 */
public class WelcomeAndGoodbye extends ListenerAdapter {

    // ==================== ENTRADA DE MEMBRO ====================

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();

        BotLogger.info("Novo membro entrou: " + member.getEffectiveName() + " (ID: " + member.getId() + ")");

        try {
            // atribuir cargo automático
            assignAutoRole(guild, member);

            // mensagem de boas-vindas
            sendWelcomeMessage(guild, member);

        } catch (Exception e) {
            BotLogger.error("Erro ao processar entrada de membro", e);
        }
    }

    /**
     * Atribui cargo automático ao novo membro
     */
    private void assignAutoRole(Guild guild, Member member) {
        String autoRoleId = BotConfig.getAutoRoleId();

        if (autoRoleId == null || autoRoleId.isEmpty()) {
            BotLogger.warn("Auto role ID não configurado");
            return;
        }

        Role role = guild.getRoleById(autoRoleId);

        if (role == null) {
            BotLogger.error("Cargo automático não encontrado: " + autoRoleId);
            return;
        }

        BotLogger.debug("Atribuindo cargo '" + role.getName() + "' para " + member.getEffectiveName());

        guild.addRoleToMember(member, role).queue(
                success -> BotLogger.success("✅ Cargo atribuído a " + member.getEffectiveName()),
                error -> BotLogger.error("❌ Erro ao atribuir cargo: " + error.getMessage())
        );
    }

    /**
     * Envia mensagem de boas-vindas
     */
    private void sendWelcomeMessage(Guild guild, Member member) {
        String channelId = BotConfig.getWelcomeChannelId();

        if (channelId == null || channelId.isEmpty()) {
            BotLogger.warn("Welcome channel ID não configurado");
            return;
        }

        TextChannel channel = guild.getTextChannelById(channelId);

        if (channel == null) {
            BotLogger.error("Canal de boas-vindas não encontrado: " + channelId);
            return;
        }

        // cria embed de boas-vindas
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("👋 Novo membro entrou!")
                .setDescription(String.format(
                        "Gehirn, o fodão te deseja boas-vindas, %s!\n\n" +
                                "Você é o **%d°** membro do servidor!",
                        member.getAsMention(),
                        guild.getMemberCount()
                ))
                .setColor(Color.GREEN)
                .setThumbnail(member.getEffectiveAvatarUrl())
                .setFooter("Bem-vindo(a)!", guild.getIconUrl())
                .setTimestamp(java.time.Instant.now());

        BotLogger.debug("Enviando mensagem de boas-vindas para " + member.getEffectiveName());

        channel.sendMessageEmbeds(embed.build()).queue(
                success -> BotLogger.success("✅ Mensagem de boas-vindas enviada"),
                error -> BotLogger.error("❌ Erro ao enviar mensagem: " + error.getMessage())
        );
    }

    // ==================== SAÍDA DE MEMBRO ====================

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        Guild guild = event.getGuild();
        User user = event.getUser();

        BotLogger.info("Membro saiu: " + user.getName() + " (ID: " + user.getId() + ")");

        try {
            // verifica se foi ban ou saída normal
            checkBanOrLeave(guild, user);

        } catch (Exception e) {
            BotLogger.error("Erro ao processar saída de membro", e);
        }
    }

    /**
     * Verifica se foi ban, kick ou saída normal e envia mensagem
     */
    private void checkBanOrLeave(Guild guild, User user) {
        String channelId = BotConfig.getExitChannelId();

        if (channelId == null || channelId.isEmpty()) {
            BotLogger.warn("Exit channel ID não configurado");
            return;
        }

        TextChannel channel = guild.getTextChannelById(channelId);

        if (channel == null) {
            BotLogger.error("Canal de saída não encontrado: " + channelId);
            return;
        }

        BotLogger.debug("Verificando tipo de saída...");

        // verifica se foi banido
        guild.retrieveBan(user).queue(
                ban -> {
                    // se foi banido
                    BotLogger.info("Usuário foi banido: " + user.getName());
                    sendBanMessage(channel, user, ban.getReason());
                },
                errorBan -> {
                    // não foi ban, verifica se foi kick
                    BotLogger.debug("Não foi ban, verificando audit logs para kick...");
                    checkKickInAuditLog(guild, user, channel);
                }
        );
    }

    /**
     * Verifica o usuário foi kickado
     */
    private void checkKickInAuditLog(Guild guild, User user, TextChannel channel) {
        guild.retrieveAuditLogs()
                .type(net.dv8tion.jda.api.audit.ActionType.KICK)
                .limit(10) // últimas 10 ações de kick
                .queue(
                        auditLogs -> {
                            // busca por kick do usuário
                            boolean wasKicked = auditLogs.stream()
                                    .filter(entry -> entry.getTargetId().equals(user.getId()))
                                    .filter(entry -> {
                                        // Considera kick se foi nos últimos 5 segundos
                                        long timeDiff = System.currentTimeMillis() - entry.getTimeCreated().toInstant().toEpochMilli();
                                        return timeDiff < 5000; // 5 segundos
                                    })
                                    .findFirst()
                                    .map(entry -> {
                                        // foi kickado
                                        String kickedBy = entry.getUser() != null ? entry.getUser().getName() : "Desconhecido";
                                        String reason = entry.getReason();
                                        BotLogger.info("Usuário foi kickado por: " + kickedBy);
                                        sendKickMessage(channel, user, kickedBy, reason);
                                        return true;
                                    })
                                    .orElse(false);

                            if (!wasKicked) {
                                // usuário saiu normalmente
                                BotLogger.info("Usuário saiu normalmente: " + user.getName());
                                sendLeaveMessage(channel, user);
                            }
                        },
                        error -> {
                            // erro ao buscar audit logs, assume saída normal
                            BotLogger.warn("Erro ao buscar audit logs (pode ser falta de permissão): " + error.getMessage());
                            BotLogger.info("Assumindo saída normal de: " + user.getName());
                            sendLeaveMessage(channel, user);
                        }
                );
    }

    /**
     * Envia mensagem de ban
     */
    private void sendBanMessage(TextChannel channel, User user, String reason) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🔨 Gehirn, o fodão baniu alguém.")
                .setDescription(String.format(
                        "O usuário **%s** foi banido do servidor por ser um pascácio.",
                        user.getName()
                ))
                .setThumbnail(user.getEffectiveAvatarUrl())
                .setColor(Color.RED)
                .addField("📝 Motivo",
                        reason != null && !reason.isEmpty() ? reason : "Não especificado",
                        false)
                .setFooter("Banido", null)
                .setTimestamp(java.time.Instant.now());

        channel.sendMessageEmbeds(embed.build()).queue(
                success -> BotLogger.success("✅ Mensagem de ban enviada"),
                error -> BotLogger.error("❌ Erro ao enviar mensagem: " + error.getMessage())
        );
    }

    /**
     * Envia mensagem de kick
     */
    private void sendKickMessage(TextChannel channel, User user, String kickedBy, String reason) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("👢 Membro foi expulso do servidor")
                .setDescription(String.format(
                        "O usuário **%s** foi expulso do servidor por **%s**.",
                        user.getName(),
                        kickedBy
                ))
                .setThumbnail(user.getEffectiveAvatarUrl())
                .setColor(new Color(255, 140, 0))
                .addField("📝 Motivo",
                        reason != null && !reason.isEmpty() ? reason : "Não especificado",
                        false)
                .addField("👮 Expulso por", kickedBy, false)
                .setFooter("Expulso", null)
                .setTimestamp(java.time.Instant.now());

        channel.sendMessageEmbeds(embed.build()).queue(
                success -> BotLogger.success("✅ Mensagem de kick enviada"),
                error -> BotLogger.error("❌ Erro ao enviar mensagem: " + error.getMessage())
        );
    }

    /**
     * Envia mensagem de saída normal
     */
    private void sendLeaveMessage(TextChannel channel, User user) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🚪 Membro saiu do servidor")
                .setDescription(String.format(
                        "O usuário **%s** saiu do servidor por não gostar de Gehirn, o fodão... " +
                                "Mas Gehirn continua sendo fodão!",
                        user.getName()
                ))
                .setThumbnail(user.getEffectiveAvatarUrl())
                .setColor(Color.ORANGE)
                .setFooter("Até logo", null)
                .setTimestamp(java.time.Instant.now());

        channel.sendMessageEmbeds(embed.build()).queue(
                success -> BotLogger.success("✅ Mensagem de saída enviada"),
                error -> BotLogger.error("❌ Erro ao enviar mensagem: " + error.getMessage())
        );
    }
}