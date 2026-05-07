package com.example.learnlybacked.disocrd.config;

import com.example.learnlybacked.listener.MessageListener;
import com.example.learnlybacked.listener.SlashCommandListener;
import dev.arbjerg.lavalink.client.LavalinkClient;
import dev.arbjerg.lavalink.libraries.jda.JDAVoiceUpdateListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DiscordConfig {

    @Value("${discord.token}")
    private String token;

    @Bean
    public JDA jda(
            MessageListener messageListener,
            SlashCommandListener slashCommandListener,
            LavalinkClient lavalinkClient
    ) throws InterruptedException {

        JDABuilder builder = JDABuilder.createDefault(token)
                .enableIntents(
                        GatewayIntent.GUILD_VOICE_STATES,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT
                )
                .enableCache(CacheFlag.VOICE_STATE)
                .addEventListeners(messageListener, slashCommandListener);

        // Ustawienie interceptora dla JDA
        builder.setVoiceDispatchInterceptor(new JDAVoiceUpdateListener(lavalinkClient));

        JDA jda = builder.build().awaitReady();

        String guildId = "944711940771561512";
        Guild guild = jda.getGuildById(guildId);

        if (guild != null) {
            guild.updateCommands().addCommands(
                    Commands.slash("say", "Treść do wysłania")
                            .addOption(OptionType.STRING, "content", "Treść", true),
                    Commands.slash("roll", "Rzut kośćmi")
                            .addOption(OptionType.STRING, "roll", "Liczba kości i ścian (np. 3d6)", true)
                            .addOption(OptionType.STRING, "roll_bonus", "Bonus do rzutu (np. +2)", false),
                    Commands.slash("join", "Dołącz do kanału głosowego."),
                    Commands.slash("leave", "Wyjdź z kanału głosowego."),
                    Commands.slash("stop", "Zatrzymaj odtwarzanie."),
                    Commands.slash("resume", "Wznów odtwarzacz."),
                    Commands.slash("play", "Odtwórz utwór")
                            .addOption(OptionType.STRING, "identifier", "Identyfikator utworu lub link", true)
            ).queue();
            System.out.println("Komendy zarejestrowane dla serwera!");
        }

        jda.updateCommands().addCommands(
                Commands.slash("say", "Treść do wysłania")
                        .addOption(OptionType.STRING, "content", "Treść", true),
                Commands.slash("roll", "Rzut kośćmi")
                        .addOption(OptionType.STRING, "roll", "Liczba kości i ścian (np. 3d6)", true)
                        .addOption(OptionType.STRING, "roll_bonus", "Bonus do rzutu (np. +2)", false),
                Commands.slash("join", "Dołącz do kanału głosowego."),
                Commands.slash("leave", "Wyjdź z kanału głosowego."),
                Commands.slash("stop", "Zatrzymaj odtwarzanie."),
                Commands.slash("resume", "Wznów odtwarzacz."),
                Commands.slash("play", "Odtwórz utwór")
                        .addOption(OptionType.STRING, "identifier", "Identyfikator utworu lub link", true)
        ).queue();
        System.out.println("Komendy globalne zarejestrowane dla wszystkich serwerów!");

        return jda;
    }
}