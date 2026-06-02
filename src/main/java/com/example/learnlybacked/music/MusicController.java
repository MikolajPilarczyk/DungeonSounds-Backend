package com.example.learnlybacked.music;

import dev.arbjerg.lavalink.client.FunctionalLoadResultHandler;
import dev.arbjerg.lavalink.client.LavalinkClient;
import dev.arbjerg.lavalink.client.Link;
import dev.arbjerg.lavalink.client.player.Track;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api")
@Service
public class MusicController {

    private final LavalinkClient client;
    private final JDA jda;

    @Getter
    @Setter
    public static class SongData {
        private Long trackId;
        private String trackTitle;
        private String trackUrl;
        private String guildId;
        private String userId;
    }

    @Getter
    @Setter
    public static class ControlData {
        private String guildId;
    }

    public MusicController(LavalinkClient client, @Lazy JDA jda) {
        this.client = client;
        this.jda = jda;
    }

    public record SongDurationData(
            String durationMs,   // długość w ms jako String
            String title
    ) {}



    public record PauseStateData(boolean paused) {}

    @PostMapping("/pause")
    public ResponseEntity<PauseStateData> pauseTrack(@RequestBody ControlData data) {
        if (data.getGuildId() == null || data.getGuildId().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            long guildId = Long.parseLong(data.getGuildId());
            PauseMusic(guildId);
            return ResponseEntity.ok(new PauseStateData(true));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/resume")
    public ResponseEntity<PauseStateData> resumeTrack(@RequestBody ControlData data) {
        if (data.getGuildId() == null || data.getGuildId().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            long guildId = Long.parseLong(data.getGuildId());
            ResumeMusic(guildId);
            return ResponseEntity.ok(new PauseStateData(false));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().build();
        }
    }


    @PostMapping("/playsong")
    public ResponseEntity<SongDurationData> RecivePlaylistSet(@RequestBody SongData songData) {
        System.out.println("Wywołano piosenkę z web: " + songData.getGuildId() + " " + songData.getTrackTitle());

        // Walidacja obecności wymaganych danych tekstowych
        if (songData.getGuildId() == null || songData.getGuildId().isEmpty()) {
            System.err.println("[API] Błąd: Przesłane guildId jest puste.");
        }
        if (songData.getUserId() == null || songData.getUserId().isEmpty()) {
            System.err.println("[API] Błąd: Przesłane userId jest puste.");
        }

        try {
            long guildId = Long.parseLong(songData.getGuildId());
            long userId = Long.parseLong(songData.getUserId());

            Long channelId = getUserVoiceChannelId(guildId, userId);
            if (channelId == null) {
                return ResponseEntity.badRequest().build();
            }

            JoinChannel(guildId, channelId);

            String trackLink = songData.getTrackUrl();
            if (trackLink == null || trackLink.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            SongDurationData songDurationData = playMusic(guildId, trackLink);

            if (songDurationData == null) {
                return ResponseEntity.status(404).build();
            }

            return ResponseEntity.ok(songDurationData); // <-- zwraca JSON z danymi

        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().build();
        }
    }


    public Long getUserVoiceChannelId(long guildId, long userId) {
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            System.err.println("[JDA] Nie znaleziono gildii o ID: " + guildId);
            return null;
        }

        Member member = guild.getMemberById(userId);
        if (member == null) {
            System.err.println("[JDA] Nie znaleziono użytkownika o ID: " + userId + " na tym serwerze.");
            return null;
        }

        GuildVoiceState voiceState = member.getVoiceState();
        if (voiceState != null && voiceState.inAudioChannel() && voiceState.getChannel() != null) {
            return voiceState.getChannel().getIdLong();
        }

        System.out.println("[JDA] Użytkownik " + member.getEffectiveName() + " nie jest na żadnym kanale głosowym.");
        return null;
    }

    public void JoinChannel(long guildId, long channelId) {
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            System.err.println("[JDA] Nie znaleziono gildii o ID: " + guildId);
            return;
        }

        VoiceChannel voiceChannel = guild.getVoiceChannelById(channelId);
        if (voiceChannel == null) {
            System.err.println("[JDA] Nie znaleziono kanału głosowego o ID: " + channelId);
            return;
        }

        guild.getAudioManager().openAudioConnection(voiceChannel);
        System.out.println("[JDA] Bot pomyślnie dołączył do kanału: " + voiceChannel.getName());
    }

    public void PauseMusic(long guildId) {
        System.out.println("Wywołano PauseMusic dla: " + guildId);
        var link = this.client.getOrCreateLink(guildId);

        link.getPlayer().flatMap(player -> player.setPaused(true))
                .subscribe(
                        updatedPlayer -> System.out.println("Muzyka została zapauzowana."),
                        error -> System.err.println("Błąd pauzowania: " + error.getMessage())
                );
    }

    public void ResumeMusic(long guildId) {
        System.out.println("Wywołano ResumeMusic dla: " + guildId);
        var link = this.client.getOrCreateLink(guildId);

        link.getPlayer().flatMap(player -> player.setPaused(false))
                .subscribe(
                        updatedPlayer -> System.out.println("Muzyka została odpauzowana."),
                        error -> System.err.println("Błąd odpauzowania: " + error.getMessage())
                );
    }

    public SongDurationData playMusic(long guildId, String trackLink) {
        System.out.println("[Lavalink] Próba uruchomienia utworu dla gildii " + guildId + ": " + trackLink);
        final Link link = this.client.getOrCreateLink(guildId);

        CompletableFuture<SongDurationData> future = new CompletableFuture<>();

        link.loadItem(trackLink).subscribe(new FunctionalLoadResultHandler(
                (trackLoad) -> {
                    final Track track = trackLoad.getTrack();
                    link.createOrUpdatePlayer()
                            .setTrack(track)
                            .setVolume(75)
                            .subscribe(
                                    (player) -> {
                                        System.out.println("[Lavalink] Odtwarzam: " + track.getInfo().getTitle());
                                        future.complete(buildDurationData(track));
                                    },
                                    (error) -> {
                                        System.err.println("[Lavalink] Błąd ustawiania tracka: " + error.getMessage());
                                        future.completeExceptionally(error);
                                    }
                            );
                },
                (playlistLoad) -> {
                    final List<Track> tracks = playlistLoad.getTracks();
                    if (tracks.isEmpty()) {
                        System.out.println("[Lavalink] Playlista jest pusta.");
                        future.complete(null);
                        return;
                    }
                    final Track firstTrack = tracks.get(0);
                    link.createOrUpdatePlayer()
                            .setTrack(firstTrack)
                            .setVolume(75)
                            .subscribe(
                                    (player) -> {
                                        System.out.println("[Lavalink] Odtwarzam z playlisty: " + firstTrack.getInfo().getTitle());
                                        future.complete(buildDurationData(firstTrack));
                                    },
                                    (error) -> {
                                        System.err.println("[Lavalink] Błąd playlisty: " + error.getMessage());
                                        future.completeExceptionally(error);
                                    }
                            );
                },
                (searchLoad) -> {
                    final List<Track> tracks = searchLoad.getTracks();
                    if (tracks.isEmpty()) {
                        System.out.println("[Lavalink] Nie znaleziono wyników wyszukiwania.");
                        future.complete(null);
                        return;
                    }
                    final Track firstTrack = tracks.get(0);
                    link.createOrUpdatePlayer()
                            .setTrack(firstTrack)
                            .setVolume(75)
                            .subscribe(
                                    (player) -> {
                                        System.out.println("[Lavalink] Odtwarzam z wyszukiwarki: " + firstTrack.getInfo().getTitle());
                                        future.complete(buildDurationData(firstTrack));
                                    },
                                    (error) -> {
                                        System.err.println("[Lavalink] Błąd wyszukiwania: " + error.getMessage());
                                        future.completeExceptionally(error);
                                    }
                            );
                },
                () -> {
                    System.out.println("[Lavalink] Nie znaleziono utworu.");
                    future.complete(null);
                },
                (loadFailed) -> {
                    String reason = loadFailed.getException().getMessage();
                    System.err.println("[Lavalink] Błąd ładowania: " + reason);
                }
        ));

        try {
            return future.get(10, TimeUnit.SECONDS); // timeout 10s
        } catch (Exception e) {
            System.err.println("[Lavalink] Timeout lub błąd: " + e.getMessage());
            return null;
        }
    }

    private SongDurationData buildDurationData(Track track) {
        long durationMs = track.getInfo().getLength();
        return new SongDurationData(
                String.valueOf(durationMs),
                track.getInfo().getTitle()
        );
    }


}