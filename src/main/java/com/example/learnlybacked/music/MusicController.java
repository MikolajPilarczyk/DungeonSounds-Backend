package com.example.learnlybacked.music;

import dev.arbjerg.lavalink.client.FunctionalLoadResultHandler;
import dev.arbjerg.lavalink.client.LavalinkClient;
import dev.arbjerg.lavalink.client.Link;
import dev.arbjerg.lavalink.client.player.Track;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api")
@Service
public class MusicController {

    private final LavalinkClient client;

    @Getter
    @Setter
    public static class SongData {
        private Long trackId;
        private String trackTitle;
        private String trackUrl;
        private String guildId;
    }

    public MusicController(LavalinkClient client) {
        this.client = client;
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

    @PostMapping("/playsong")
    public void RecivePlaylistSet(@RequestBody SongData songData) {
        System.out.println("Wywołano piosenkę z web: " + songData.getGuildId() + " " + songData.getTrackTitle());

        try {
            long guildId = Long.parseLong(songData.getGuildId());
            String trackLink = songData.getTrackUrl();
            PlayMusic(guildId, trackLink);
        } catch (NumberFormatException e) {
            System.err.println("Błąd: Niepoprawny format guildId: " + songData.getGuildId());
        }
    }

    public void ReciveUserGuild(String discordId) {
    }
}