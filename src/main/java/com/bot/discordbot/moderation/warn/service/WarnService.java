package com.bot.discordbot.moderation.warn.service;

import com.bot.discordbot.moderation.warn.dao.WarnDAO;
import com.bot.discordbot.moderation.warn.model.Warn;
import com.bot.discordbot.util.BotLogger;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.time.Duration;
import java.util.List;

/**
 * Serviço que gerencia a lógica de negócio relacionada aos warns
 * há validações, regras de expiração e aplicação de punições.
 */
public class WarnService {

    // ==================== CONSTANTES ====================

    private static final int MAX_REASON_LENGTH = 500;
    private static final int MAX_WARNS_BEFORE_BAN = 6;

    // ==================== PÚBLICO ====================

    /**
     * Adiciona um warn a um usuário com todas as validações necessárias
     */
    public static boolean addWarn(String userId, String moderatorId, String reason, Guild guild) {
        BotLogger.debug("=== INÍCIO addWarn ===");
        BotLogger.debug("userId: " + userId);
        BotLogger.debug("moderatorId: " + moderatorId);
        BotLogger.debug("reason: " + reason);

        // validações
        if (!validateWarnInput(userId, reason)) {
            BotLogger.error("Validação de input falhou");
            return false;
        }

        // limpa os warns expirados antes de contar
        int purged = WarnDAO.purgeExpiredWarns();
        BotLogger.debug("Warns expirados removidos: " + purged);

        // verifica warns atuais
        int currentWarns = WarnDAO.countActiveWarns(userId);
        int newWarnCount = currentWarns + 1;
        BotLogger.debug("Warns atuais: " + currentWarns + " | Novo count: " + newWarnCount);

        // calcula timestamps
        long now = System.currentTimeMillis();
        long expiresAt = calculateExpirationMillis(newWarnCount, now);
        BotLogger.debug("Timestamp now: " + now);
        BotLogger.debug("Expira em: " + expiresAt);
        BotLogger.debug("Diferença (ms): " + (expiresAt - now));

        // persiste no banco
        boolean success = WarnDAO.addWarn(userId, moderatorId, reason, now, expiresAt);
        BotLogger.debug("Persistência no banco: " + (success ? "SUCESSO" : "FALHA"));

        if (success) {
            // verifica se realmente foi salvo
            int verifyCount = WarnDAO.countActiveWarns(userId);
            BotLogger.debug("Verificação pós-inserção: " + verifyCount + " warns ativos");

            BotLogger.moderation("WARN", userId, moderatorId, reason);
            BotLogger.info(String.format("Warn %d/%d aplicado ao usuário %s",
                    newWarnCount, MAX_WARNS_BEFORE_BAN, userId));

            // aplica punição automática de forma assíncrona
            if (guild != null) {
                applyPunishmentAsync(newWarnCount, userId, guild);
            }
        } else {
            BotLogger.error("Falha ao persistir warn no banco de dados");
        }

        BotLogger.debug("=== FIM addWarn ===");
        return success;
    }

    /**
     * Retorna warns ativos de um usuário
     */
    public static List<Warn> getActiveWarns(String userId) {
        if (userId == null || userId.isEmpty()) {
            BotLogger.warn("Tentativa de buscar warns com userId inválido");
            return List.of();
        }
        return WarnDAO.getActiveWarns(userId);
    }

    /**
     * Retorna o histórico de warns
     */
    public static List<Warn> getWarnHistory(String userId) {
        if (userId == null || userId.isEmpty()) {
            BotLogger.warn("Tentativa de buscar histórico com userId inválido");
            return List.of();
        }
        return WarnDAO.getWarnHistory(userId);
    }

    /**
     * Remove todos os warns de um usuário
     */
    public static int clearUserWarns(String userId) {
        if (userId == null || userId.isEmpty()) {
            BotLogger.warn("Tentativa de limpar warns com userId inválido");
            return 0;
        }

        int removed = WarnDAO.clearUserWarns(userId);
        if (removed > 0) {
            BotLogger.moderation("WARN_CLEAR", userId, "SYSTEM",
                    removed + " warns removidos");
        }
        return removed;
    }

    /**
     * Remove um warn específico por ID
     */
    public static boolean removeWarnById(int warnId) {
        boolean success = WarnDAO.removeWarnById(warnId);
        if (success) {
            BotLogger.info("Warn ID " + warnId + " removido manualmente");
        }
        return success;
    }

    /**
     * Limpa warns expirados do sistema
     */
    public static int purgeExpiredWarns() {
        int purged = WarnDAO.purgeExpiredWarns();
        if (purged > 0) {
            BotLogger.info("Purge automático removeu " + purged + " warns expirados");
        }
        return purged;
    }

    // ==================== VALIDAÇÕES ====================

    /**
     * Valida entrada de dados para warn
     */
    private static boolean validateWarnInput(String userId, String reason) {
        if (userId == null || userId.isEmpty()) {
            BotLogger.error("userId nulo ou vazio ao adicionar warn");
            return false;
        }

        if (reason == null || reason.trim().isEmpty()) {
            BotLogger.warn("Tentativa de adicionar warn sem motivo");
            return false;
        }

        if (reason.length() > MAX_REASON_LENGTH) {
            BotLogger.warn("Motivo de warn excede limite de caracteres: " + reason.length());
            return false;
        }

        return true;
    }

    /**
     * Verifica se membro pode receber warns
     * Imunidade: bots, moderadores, administradores, etc
     */
    private static boolean canReceiveWarn(Member member) {
        if (member == null) {
            return true; // Se não encontrou o membro, permite (pode ter saído)
        }

        // bots não recebem warns
        if (member.getUser().isBot()) {
            BotLogger.debug("Bot detectado, ignorando warn: " + member.getId());
            return false;
        }

        // staff não recebe warns
        if (hasModeratorPermissions(member)) {
            BotLogger.debug("Membro com permissões de moderação, ignorando warn: " + member.getId());
            return false;
        }

        return true;
    }

    /**
     * Verifica se membro tem permissões de moderação
     */
    private static boolean hasModeratorPermissions(Member member) {
        return member.hasPermission(Permission.MODERATE_MEMBERS) ||
                member.hasPermission(Permission.KICK_MEMBERS) ||
                member.hasPermission(Permission.BAN_MEMBERS) ||
                member.hasPermission(Permission.ADMINISTRATOR);
    }

    // ==================== REGRAS DE EXPIRAÇÃO ====================

    /**
     * Calcula quando o warn deve expirar baseado na quantidade
     *
     * Regras:
     * 1º warn -> 24 horas
     * 2º warn -> 48 horas
     * 3º warn -> 7 dias
     * 4º warn -> 14 dias
     * 5º warn -> 30 dias
     */
    private static long calculateExpirationMillis(int warnCount, long referenceTime) {
        Duration duration = switch (warnCount) {
            case 1 -> Duration.ofHours(24);
            case 2 -> Duration.ofHours(48);
            case 3 -> Duration.ofDays(7);
            case 4 -> Duration.ofDays(14);
            default -> Duration.ofDays(30);
        };

        long expiresAt = referenceTime + duration.toMillis();
        BotLogger.debug(String.format("Warn %d expirará em: %s",
                warnCount, duration.toString()));

        return expiresAt;
    }

    // ==================== PUNIÇÕES AUTOMÁTICAS ====================

    /**
     * Aplica punições
     */
    private static void applyPunishmentAsync(int warnCount, String targetUserId, Guild guild) {
        if (guild == null) {
            BotLogger.warn("Guild nula, não foi possível aplicar punição");
            return;
        }

        BotLogger.debug("Iniciando aplicação assíncrona de punição para warn #" + warnCount);

        // busca o membro de forma assíncrona
        guild.retrieveMemberById(targetUserId).queue(
                target -> {
                    BotLogger.debug("Membro encontrado para punição: " + target.getEffectiveName());

                    // não pune bots ou staff
                    if (!canReceiveWarn(target)) {
                        BotLogger.debug("Membro imune a punições, ignorando");
                        return;
                    }

                    // aplica a punição
                    applyPunishment(warnCount, target, guild);
                },
                error -> {
                    BotLogger.warn("Erro ao buscar membro para punição: " + error.getMessage());
                    BotLogger.info("Warn foi registrado, mas punição não pôde ser aplicada (usuário pode ter saído)");
                }
        );
    }

    /**
     * Aplica a punição específica baseada no número de warns
     */
    private static void applyPunishment(int warnCount, Member target, Guild guild) {
        String reason = String.format("Punição automática: %dº warn", warnCount);
        String targetUserId = target.getId();

        try {
            switch (warnCount) {
                case 1 -> {
                    BotLogger.info("Warn 1/6 aplicado - Apenas aviso (sem punição)");
                }
                case 2 -> {
                    BotLogger.info("Aplicando timeout de 10 minutos para " + target.getEffectiveName());
                    target.timeoutFor(Duration.ofMinutes(10))
                            .reason(reason)
                            .queue(
                                    success -> BotLogger.success("✅ Timeout 10min aplicado: " + targetUserId),
                                    error -> BotLogger.error("❌ Erro ao aplicar timeout: " + error.getMessage())
                            );
                }
                case 3 -> {
                    BotLogger.info("Aplicando timeout de 1 hora para " + target.getEffectiveName());
                    target.timeoutFor(Duration.ofHours(1))
                            .reason(reason)
                            .queue(
                                    success -> BotLogger.success("✅ Timeout 1h aplicado: " + targetUserId),
                                    error -> BotLogger.error("❌ Erro ao aplicar timeout: " + error.getMessage())
                            );
                }
                case 4 -> {
                    BotLogger.info("Aplicando timeout de 24 horas para " + target.getEffectiveName());
                    target.timeoutFor(Duration.ofDays(1))
                            .reason(reason)
                            .queue(
                                    success -> BotLogger.success("✅ Timeout 24h aplicado: " + targetUserId),
                                    error -> BotLogger.error("❌ Erro ao aplicar timeout: " + error.getMessage())
                            );
                }
                case 5 -> {
                    String lastWarning = reason + " - ÚLTIMO AVISO ANTES DO BAN";
                    BotLogger.warn("Aplicando timeout de 3 dias (último aviso) para " + target.getEffectiveName());
                    target.timeoutFor(Duration.ofDays(3))
                            .reason(lastWarning)
                            .queue(
                                    success -> BotLogger.warn("⚠️ Timeout 3d (último aviso) aplicado: " + targetUserId),
                                    error -> BotLogger.error("❌ Erro ao aplicar timeout: " + error.getMessage())
                            );
                }
                case 6 -> {
                    BotLogger.error("APLICANDO BAN para " + target.getEffectiveName() + " (6 warns)");
                    guild.ban(target, 0, java.util.concurrent.TimeUnit.DAYS)
                            .reason("Banimento automático: " + MAX_WARNS_BEFORE_BAN + " warns acumulados")
                            .queue(
                                    success -> BotLogger.error("🔨 BAN APLICADO: " + targetUserId + " atingiu 6 warns"),
                                    error -> BotLogger.error("❌ Erro ao aplicar ban: " + error.getMessage())
                            );
                }
                default -> {
                    BotLogger.warn("Warn count " + warnCount + " excede máximo esperado");
                }
            }
        } catch (Exception e) {
            BotLogger.error("Exceção ao aplicar punição automática", e);
        }
    }

    // ==================== UTILITÁRIOS ====================

    /**
     * Retorna descrição textual do nível de punição
     */
    public static String getPunishmentDescription(int warnCount) {
        return switch (warnCount) {
            case 1 -> "⚠️ Primeiro aviso - Sem punição";
            case 2 -> "🕐 Timeout de 10 minutos";
            case 3 -> "🕐 Timeout de 1 hora";
            case 4 -> "🕐 Timeout de 24 horas";
            case 5 -> "⚠️ Timeout de 3 dias - ÚLTIMO AVISO";
            case 6 -> "🔨 BAN PERMANENTE";
            default -> "🔴 Sistema de punição excedido";
        };
    }

    /**
     * Retorna informações sobre o sistema
     */
    public static String getWarnSystemInfo() {
        return """
            📋 **Sistema de Warns**
            
            **Expiração:**
            • 1º warn → 24 horas
            • 2º warn → 48 horas
            • 3º warn → 7 dias
            • 4º warn → 14 dias
            • 5º warn → 30 dias
            • 6º+ warn → 365 dias
            
            **Punições:**
            • 1 warn → Aviso
            • 2 warns → Timeout 10min
            • 3 warns → Timeout 1h
            • 4 warns → Timeout 24h
            • 5 warns → Timeout 3d (último aviso)
            • 6 warns → Ban permanente
            """;
    }
}