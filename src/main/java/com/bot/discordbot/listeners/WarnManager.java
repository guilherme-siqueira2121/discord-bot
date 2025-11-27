package com.bot.discordbot.listeners;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gerencia warns por usuário.
 * Armazena uma lista de timestamps (em millis) para cada warn recebido.
 * Expiração é calculada a partir do warn mais recente e do número de warns válidos.
 */
public class WarnManager {

    // userId -> lista de timestamps (ordem crescente)
    private static final Map<String, List<Long>> warns = new HashMap<>();

    // ---------- API pública ----------

    /**
     * Adiciona um warn para o usuário e retorna o número atual de warns válidos.
     */
    public static synchronized int addWarn(String userId) {
        long agora = System.currentTimeMillis();

        // remove warns expirados antes de adicionar
        limparWarnsExpirados(userId, agora);

        List<Long> list = warns.computeIfAbsent(userId, k -> new ArrayList<>());
        list.add(agora);
        return list.size();
    }

    /**
     * Retorna o número de warns válidos atualmente (aplica limpeza antes).
     */
    public static synchronized int getWarns(String userId) {
        limparWarnsExpirados(userId, System.currentTimeMillis());
        List<Long> list = warns.get(userId);
        return list == null ? 0 : list.size();
    }

    /**
     * Retorna a timestamp (ms) do último warn (mais recente), ou 0 se não houver.
     */
    public static synchronized long getLastWarnTimestamp(String userId) {
        limparWarnsExpirados(userId, System.currentTimeMillis());
        List<Long> list = warns.get(userId);
        if (list == null || list.isEmpty()) return 0L;
        return list.get(list.size() - 1);
    }

    /**
     * Retorna uma cópia da lista de timestamps (em ms) dos warns válidos.
     */
    public static synchronized List<Long> getWarnTimestamps(String userId) {
        limparWarnsExpirados(userId, System.currentTimeMillis());
        List<Long> list = warns.get(userId);
        if (list == null) return Collections.emptyList();
        return new ArrayList<>(list);
    }

    /**
     * Calcula quanto tempo (ms) falta até o reset completo baseado no número de warns atuais.
     * Se o usuário não tiver warns, retorna 0.
     */
    public static synchronized long getTimeUntilReset(String userId) {
        long agora = System.currentTimeMillis();
        List<Long> list = warns.get(userId);
        if (list == null || list.isEmpty()) return 0L;

        int currentWarns = list.size();
        long last = list.get(list.size() - 1);
        long periodo = calcularTempoReset(currentWarns);
        long expireAt = last + periodo;
        long restante = expireAt - agora;
        return Math.max(0L, restante);
    }

    /**
     * Método público para calcular o tempo de reset (ms) com escalonamento.
     * Base = 12 horas, multiplicador exponencial 1.5^(warns-1)
     */
    public static long calcularTempoReset(int warnsCount) {
        if (warnsCount <= 0) return 0L;
        long base = 12L * 60 * 60 * 1000; // 12 horas em ms
        double multiplicador = Math.pow(1.5, warnsCount - 1);
        return (long) (base * multiplicador);
    }

    /**
     * Reseta (remove) todos os warns do usuário.
     */
    public static synchronized void resetWarns(String userId) {
        warns.remove(userId);
    }

    // ---------- funções internas ----------

    /**
     * Remove warns que já expiraram, com base no tempo corrente passado.
     */
    private static void limparWarnsExpirados(String userId, long nowMillis) {
        List<Long> list = warns.get(userId);
        if (list == null || list.isEmpty()) return;

        // remove from beginning while the warn is expired relative to its position
        // IMPORTANT: expiração depende de quantos warns o usuário tem atualmente.
        // Simples estratégia: re-calcular removendo warns antigos repetidamente.
        boolean changed;
        do {
            changed = false;
            int size = list.size();
            if (size == 0) break;

            // calcular periodo atual com o tamanho atual da lista
            long periodo = calcularTempoReset(size);
            long lastTimestamp = list.get(size - 1);
            long expireAt = lastTimestamp + periodo;
            if (nowMillis >= expireAt) {
                // todos os warns expiraram; limpar
                warns.remove(userId);
                changed = false;
                break;
            } else {
                // verifica se o primeiro warn já não faz mais sentido
                // recomputamos removendo o mais antigo e verificando novamente
                long first = list.get(0);
                // se ao retirar o primeiro, o novo periodo com size-1 faz com que expire também, vamos remover
                if (size - 1 > 0) {
                    long newPeriodo = calcularTempoReset(size - 1);
                    long newLast = list.get(size - 1); // same last
                    long newExpireAt = newLast + newPeriodo;
                    if (nowMillis >= newExpireAt) {
                        // remover o primeiro warn
                        list.remove(0);
                        changed = true;
                    }
                }
            }
        } while (changed);

        // Caso extremo: também garanta que warns com timestamps individuais antigos (mais de, por ex, 30 dias) sejam removidos.
        // (opcional — não necessário agora)
    }
}
