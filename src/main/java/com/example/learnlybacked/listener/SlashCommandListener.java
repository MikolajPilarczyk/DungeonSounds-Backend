package com.example.learnlybacked.listener;

import dev.arbjerg.lavalink.client.FunctionalLoadResultHandler;
import dev.arbjerg.lavalink.client.LavalinkClient;
import dev.arbjerg.lavalink.client.Link;
import dev.arbjerg.lavalink.client.player.Track;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
public class SlashCommandListener extends ListenerAdapter {

    @Autowired
    public LavalinkClient client;

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "say" -> {
                String content = event.getOption("content", OptionMapping::getAsString);
                event.reply(content).queue();
            }

            case "roll" -> {
                String roll = event.getOption("roll", OptionMapping::getAsString).trim();
                OptionMapping rollBonusOption = event.getOption("roll_bonus");
                String rollBonus = rollBonusOption != null ? rollBonusOption.getAsString().trim() : "";

                String[] rollParams = roll.split("d");

                if (rollParams.length == 2) {
                    try {
                        int count = Integer.parseInt(rollParams[0]);
                        int rollValue = Integer.parseInt(rollParams[1]);
                        int maxResult = 0;
                        Random random = new Random();

                        for (int i = 1; i <= count; i++) {
                            int singleRoll = random.nextInt(rollValue) + 1;
                            maxResult += singleRoll;
                            event.getChannel().sendMessage("Your " + i + " roll is " + singleRoll + "!").queue();
                        }

                        if (!rollBonus.isEmpty()) {
                            if (rollBonus.charAt(0) == '+') {
                                maxResult += Integer.parseInt(rollBonus.substring(1));
                            } else if (rollBonus.charAt(0) == '-') {
                                maxResult -= Integer.parseInt(rollBonus.substring(1));
                            }
                        }

                        event.reply("Rolled " + roll + " " + rollBonus + "\nFinal result: " + maxResult).queue();
                    } catch (NumberFormatException e) {
                        event.reply("Podaj rzut w poprawnym formacie").queue();
                    }
                } else {
                    event.reply("Podaj rzut w poprawnym formacie").queue();
                }
            }

            case "join" -> {
                event.deferReply(false).queue();
                joinHelper(event);
            }

            case "leave" -> {
                event.deferReply().queue();

                // Sprawdzenie stanu głosowego
                if (event.getGuild().getSelfMember().getVoiceState() == null ||
                        event.getGuild().getSelfMember().getVoiceState().getChannel() == null) {
                    event.getHook().sendMessage("Nie jestem na żadnym kanale głosowym.").queue();
                    return;
                }

                final long guildId = event.getGuild().getIdLong();

                this.client.getOrCreateLink(guildId)
                        .destroy()
                        .subscribe(
                                (ignored) -> {

                                },
                                (error) -> {
                                    event.getHook().sendMessage("Błąd: " + error.getMessage()).queue();
                                }
                        );
                event.getGuild().getAudioManager().closeAudioConnection();

                event.getHook().sendMessage("Pomyślnie opuszczono kanał!").queue();
            }

            case "stop" -> {
                event.deferReply(false).queue();
                final long guildId = event.getGuild().getIdLong();

                this.client.getOrCreateLink(guildId)
                        .destroy()
                        .subscribe(
                                (ignored) -> event.getHook().sendMessage("Stopped the current track").queue(),
                                (error) -> event.getHook().sendMessage("Błąd: " + error.getMessage()).queue()
                        );
            }



            case "play" -> handlePlayCommand(event);
        }
    }

    private void handlePlayCommand(SlashCommandInteractionEvent event) {
        final Guild guild = event.getGuild();
        if (guild == null) return;

        event.deferReply(false).queue();

        final Member member = event.getMember();
        if (member == null) {
            event.getHook().sendMessage("Nie można zidentyfikować użytkownika!").queue();
            return;
        }

        final GuildVoiceState memberVoiceState = member.getVoiceState();
        if (memberVoiceState == null || !memberVoiceState.inAudioChannel()) {
            event.getHook().sendMessage("Musisz być na kanale głosowym!").queue();
            return;
        }

        // FIX: Dołącz do kanału przed załadowaniem tracka
        guild.getAudioManager().openAudioConnection(memberVoiceState.getChannel());

        final String identifier = event.getOption("identifier").getAsString();
        final long guildId = guild.getIdLong();
        final Link link = this.client.getOrCreateLink(guildId);

        // FIX: Dodano brakujący 5. handler — loadFailed
        link.loadItem(identifier).subscribe(new FunctionalLoadResultHandler(
                // 1. Pojedynczy utwór
                (trackLoad) -> {
                    final Track track = trackLoad.getTrack();
                    link.createOrUpdatePlayer()
                            .setTrack(track)
                            .setVolume(75)
                            .subscribe(
                                    (player) -> event.getHook().sendMessage("Now playing: " + track.getInfo().getTitle()).queue(),
                                    (error) -> event.getHook().sendMessage("Błąd przy odtwarzaniu: " + error.getMessage()).queue()
                            );
                },
                (playlistLoad) -> {
                    final List<Track> tracks = playlistLoad.getTracks();
                    if (tracks.isEmpty()) {
                        event.getHook().sendMessage("Brak utworów w playliście!").queue();
                        return;
                    }
                    final Track firstTrack = tracks.get(0);
                    link.createOrUpdatePlayer()
                            .setTrack(firstTrack)
                            .setVolume(75)
                            .subscribe(
                                    (player) -> event.getHook().sendMessage(
                                            "Now playing (playlist): " + firstTrack.getInfo().getTitle()
                                                    + " [+" + (tracks.size() - 1) + " more]"
                                    ).queue(),
                                    (error) -> event.getHook().sendMessage("Błąd przy odtwarzaniu playlisty: " + error.getMessage()).queue()
                            );
                },
                (searchLoad) -> {
                    final List<Track> tracks = searchLoad.getTracks();
                    if (tracks.isEmpty()) {
                        event.getHook().sendMessage("Nie znaleziono wyników!").queue();
                        return;
                    }
                    final Track firstTrack = tracks.get(0);
                    link.createOrUpdatePlayer()
                            .setTrack(firstTrack)
                            .setVolume(75)
                            .subscribe(
                                    (player) -> event.getHook().sendMessage("Now playing: " + firstTrack.getInfo().getTitle()).queue(),
                                    (error) -> event.getHook().sendMessage("Błąd przy wyszukiwaniu: " + error.getMessage()).queue()
                            );
                },
                // 4. Brak wyników
                () -> event.getHook().sendMessage("Nie znaleziono utworu dla podanego identyfikatora.").queue(),

                (loadFailed) -> {
                    String reason = loadFailed.getException().getMessage();
                    String severity = loadFailed.getException().getSeverity().toString();
                    System.err.println("[Lavalink] Load failed | Severity: " + severity + " | Reason: " + reason);
                    event.getHook().sendMessage(
                            "Błąd ładowania (`" + severity + "`): " + reason
                    ).queue();
                }

        ));
    }

    private void joinHelper(SlashCommandInteractionEvent event) {
        final Member member = event.getMember();
        if (member == null) {
            event.getHook().sendMessage("Nie można zidentyfikować użytkownika!").queue();
            return;
        }

        final GuildVoiceState memberVoiceState = member.getVoiceState();
        if (memberVoiceState != null && memberVoiceState.inAudioChannel()) {
            event.getGuild().getAudioManager().openAudioConnection(memberVoiceState.getChannel());
            event.getHook().sendMessage("Joining your channel!").queue();
        } else {
            event.getHook().sendMessage("Musisz być na kanale głosowym!").queue();
        }
    }
}