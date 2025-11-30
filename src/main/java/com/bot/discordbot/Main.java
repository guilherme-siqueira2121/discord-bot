package com.bot.discordbot;

import com.bot.discordbot.moderation.warn.commands.WarnClearCommand;
import com.bot.discordbot.moderation.warn.commands.WarnStatusCommand;
import com.bot.discordbot.moderation.warn.commands.WarnCommand;
import com.bot.discordbot.database.DatabaseSetup;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import com.bot.discordbot.commands.PingCommand;
import com.bot.discordbot.commands.NukarCommand;
import com.bot.discordbot.listeners.WelcomeAndGoodbye;

public class Main {
    public static void main(String[] args) throws Exception {

        DatabaseSetup.initialize();

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
                .addEventListeners(new WarnStatusCommand())
                .addEventListeners(new WarnCommand())
                .addEventListeners(new WarnClearCommand())

                .build();

        jda.awaitReady();

        var guild = jda.getGuildById("1442333548706136209");
        if (guild == null) {
            System.out.println("Erro: guild não encontrada!");
            return;
        }

        guild.updateCommands()
                .addCommands(
                        Commands.slash("ping", "Responde com Pong!"),
                        Commands.slash("nukar", "Apaga 1000 mensagens do canal atual!")
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS)),

                        Commands.slash("warnstatus", "Mostra seus warns (staff pode ver outros)")
                                .addOption(OptionType.USER, "user", "Usuário a ser consultado (somente staff)", false),

                        Commands.slash("warn", "Aplica um warn a um usuário.")
                                .addOption(OptionType.USER, "user", "Usuário a ser avisado", true)
                                .addOption(OptionType.STRING, "motivo", "Motivo do warn", true)
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS)),

                        Commands.slash("warnclear", "Remove todos os warns do usuário")
                                .addOption(OptionType.USER, "user", "Usuário", true)
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS))
                )
                .queue();

        System.out.println("🤖 Bot está online!");
    }
}
