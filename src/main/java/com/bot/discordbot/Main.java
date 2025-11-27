package com.bot.discordbot;

import com.bot.discordbot.commands.WarnStatusCommand;
import com.bot.discordbot.listeners.BadWordsFilter;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import com.bot.discordbot.commands.PingCommand;
import com.bot.discordbot.commands.NukarCommand;
import com.bot.discordbot.listeners.WelcomeAndGoodbye;

public class Main {
    public static void main(String[] args) throws Exception {

        String token = System.getenv("DISCORD_BOT_TOKEN");

        JDA jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .enableIntents(GatewayIntent.GUILD_MESSAGES)
                .enableIntents(GatewayIntent.GUILD_MODERATION)
                .enableIntents(GatewayIntent.GUILD_MESSAGE_REACTIONS)

                .addEventListeners(new PingCommand())
                .addEventListeners(new NukarCommand())
                .addEventListeners(new WelcomeAndGoodbye())
                .addEventListeners(new BadWordsFilter())
                .addEventListeners(new WarnStatusCommand())
                .build();

        jda.updateCommands()
                .addCommands(
                        Commands.slash("ping", "Responde com Pong!"),
                        Commands.slash("nukar", "Apaga 1000 mensagens do canal atual!"),
                        Commands.slash("ban", "Bane um usuário do servidor")
                            .addOption(OptionType.USER, "user", "Usuário a ser banido", true)
                            .addOption(OptionType.STRING, "motivo", "Motivo", false),
                        Commands.slash("warnstatus", "Mostra os warns do usuário e quando expiram")
                )
                .queue();

        System.out.println("🤖 Bot está online!");
    }
}
