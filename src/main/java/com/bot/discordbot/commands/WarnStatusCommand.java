package com.bot.discordbot.commands;

import com.bot.discordbot.listeners.WarnManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WarnStatusCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("warnstatus")) return;

        String userId = event.getUser().getId();

        int warns = WarnManager.getWarns(userId);
        if (warns == 0) {
            event.reply("⭐ Você não tem nenhum warn! Bom comportamento!").setEphemeral(true).queue();
            return;
        }

        long restanteMs = WarnManager.getTimeUntilReset(userId);
        String restante = formatarTempo(restanteMs);

        // Opcional: mostrar timestamps dos warns
        List<Long> ts = WarnManager.getWarnTimestamps(userId);

        StringBuilder sb = new StringBuilder();
        sb.append("⚠️ **Seu status de warns:**\n\n");
        sb.append("🔸 **Warns atuais:** ").append(warns).append("\n");
        sb.append("⏳ **Próximo reset em:** ").append(restante).append("\n\n");

        sb.append("📜 **Warns (timestamps):**\n");
        for (Long t : ts) {
            sb.append("- ").append(java.time.Instant.ofEpochMilli(t).toString()).append("\n");
        }

        event.reply(sb.toString()).setEphemeral(true).queue();
    }

    private String formatarTempo(long ms) {
        if (ms <= 0) return "0s";

        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms);
        long days = seconds / (24 * 3600);
        seconds %= 24 * 3600;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder s = new StringBuilder();
        if (days > 0) s.append(days).append("d ");
        if (hours > 0) s.append(hours).append("h ");
        if (minutes > 0) s.append(minutes).append("m ");
        s.append(seconds).append("s");
        return s.toString();
    }
}
