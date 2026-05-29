package com.example.learnlybacked.music;

import dev.arbjerg.lavalink.client.FunctionalLoadResultHandler;
import dev.arbjerg.lavalink.client.LavalinkClient;
import dev.arbjerg.lavalink.client.Link;
import dev.arbjerg.lavalink.client.player.Track;
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

    // --- NOWE ENDPOINTY OBSŁUGUJĄCE PAUZĘ I WZNOWIENIE ---

    @PostMapping("/pause")
    public ResponseEntity<String> pauseTrack(@RequestBody ControlData data) {
        if (data.getGuildId() == null || data.getGuildId().isEmpty()) {
            return ResponseEntity.badRequest().body("Brak guildId w żądaniu.");
        }
        try {
            long guildId = Long.parseLong(data.getGuildId());
            PauseMusic(guildId);
            return ResponseEntity.ok("Zgłoszono żądanie zapauzowania.");
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Niepoprawny format guildId.");
        }
    }

    @PostMapping("/resume")
    public ResponseEntity<String> resumeTrack(@RequestBody ControlData data) {
        if (data.getGuildId() == null || data.getGuildId().isEmpty()) {
            return ResponseEntity.badRequest().body("Brak guildId w żądaniu.");
        }
        try {
            long guildId = Long.parseLong(data.getGuildId());
            ResumeMusic(guildId);
            return ResponseEntity.ok("Zgłoszono żądanie odpauzowania.");
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Niepoprawny format guildId.");
        }
    }

    // --- ZABEZPIECZONY ENDPOINT PLAYSONG ---

    @PostMapping("/playsong")
    public ResponseEntity<String> RecivePlaylistSet(@RequestBody SongData songData) {
        System.out.println("Wywołano piosenkę z web: " + songData.getGuildId() + " " + songData.getTrackTitle());

        // Walidacja obecności wymaganych danych tekstowych
        if (songData.getGuildId() == null || songData.getGuildId().isEmpty()) {
            System.err.println("[API] Błąd: Przesłane guildId jest puste.");
            return ResponseEntity.badRequest().body("Brak guildId.");
        }
        if (songData.getUserId() == null || songData.getUserId().isEmpty()) {
            System.err.println("[API] Błąd: Przesłane userId jest puste.");
            return ResponseEntity.badRequest().body("Brak userId.");
        }

        try {
            long guildId = Long.parseLong(songData.getGuildId());
            long userId = Long.parseLong(songData.getUserId());

            Long channelId = getUserVoiceChannelId(guildId, userId);

            if (channelId == null) {
                System.err.println("[API] Anulowano: Użytkownik nie znajduje się na kanale głosowym.");
                return ResponseEntity.badRequest().body("Użytkownik nie jest na kanale głosowym.");
            }

            JoinChannel(guildId, channelId);

            String trackLink = songData.getTrackUrl();
            if (trackLink == null || trackLink.isEmpty()) {
                System.err.println("[API] Błąd: Link do utworu jest pusty.");
                return ResponseEntity.badRequest().body("Brak linku do utworu.");
            }

            PlayMusic(guildId, trackLink);
            return ResponseEntity.ok("Piosenka została wysłana do bota.");

        } catch (NumberFormatException e) {
            System.err.println("Błąd: Niepoprawny format danych (guildId/userId): " + e.getMessage());
            return ResponseEntity.badRequest().body("Niepoprawny format ID.");
        }
    }

    // --- POZOSTAŁE METODY POMOCNICZE (BEZ ZMIAN) ---

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

    public void PlayMusic(long guildId, String trackLink) {
        System.out.println("[Lavalink] Próba uruchomienia utworu dla gildii " + guildId + ": " + trackLink);
        final Link link = this.client.getOrCreateLink(guildId);

        link.loadItem(trackLink).subscribe(new FunctionalLoadResultHandler(
                (trackLoad) -> {
                    final Track track = trackLoad.getTrack();
                    link.createOrUpdatePlayer()
                            .setTrack(track)
                            .setVolume(75)
                            .subscribe(
                                    (player) -> System.out.println("[Lavalink] Odtwarzam: " + track.getInfo().getTitle()),
                                    (error) -> System.err.println("[Lavalink] Błąd ustawiania tracka: " + error.getMessage())
                            );
                },
                (playlistLoad) -> {
                    final List<Track> tracks = playlistLoad.getTracks();
                    if (tracks.isEmpty()) {
                        System.out.println("[Lavalink] Playlista jest pusta.");
                        return;
                    }
                    final Track firstTrack = tracks.get(0);
                    link.createOrUpdatePlayer()
                            .setTrack(firstTrack)
                            .setVolume(75)
                            .subscribe(
                                    (player) -> System.out.println("[Lavalink] Odtwarzam z playlisty: " + firstTrack.getInfo().getTitle()),
                                    (error) -> System.err.println("[Lavalink] Błąd playlisty: " + error.getMessage())
                            );
                },
                (searchLoad) -> {
                    final List<Track> tracks = searchLoad.getTracks();
                    if (tracks.isEmpty()) {
                        System.out.println("[Lavalink] Nie znaleziono wyników wyszukiwania.");
                        return;
                    }
                    final Track firstTrack = tracks.get(0);
                    link.createOrUpdatePlayer()
                            .setTrack(firstTrack)
                            .setVolume(75)
                            .subscribe(
                                    (player) -> System.out.println("[Lavalink] Odtwarzam z wyszukiwarki: " + firstTrack.getInfo().getTitle()),
                                    (error) -> System.err.println("[Lavalink] Błąd wyszukiwania: " + error.getMessage())
                            );
                },
                () -> System.out.println("[Lavalink] Nie znaleziono utworu dla podanego identyfikatora."),
                (loadFailed) -> {
                    String reason = loadFailed.getException().getMessage();
                    System.err.println("[Lavalink] Błąd ładowania utworu. Powód: " + reason);
                }
        ));
    }

    public void ReciveUserGuild(String discordId) {
    }
}