package com.example.learnlybacked.playlists;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class SoundCloudService {

    private final WebClient webClient;
    private final String clientId = "TWÓJ_CLIENT_ID_Z_SOUNDCLOUDA";

    public SoundCloudService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public Long getTrackDurationInSeconds(String trackUrl) {
        try {
            String oEmbedUrl = "https://soundcloud.com/oembed?url=" + trackUrl + "&format=json";

            PlaylistController.SoundCloudTrackDto trackData = webClient.get()
                    .uri(oEmbedUrl)
                    .retrieve()
                    .bodyToMono(PlaylistController.SoundCloudTrackDto.class)
                    .block(); // .block() używamy, jeśli Twój backend jest synchroniczny (np. Spring Web-MVC)

            if (trackData == null || trackData.id() == null) {
                throw new RuntimeException("Nie udało się znaleźć utworu");
            }

            String apiUrl = "https://api.soundcloud.com/tracks/" + trackData.id() + "?client_id=" + clientId;

            PlaylistController.SoundCloudTrackDto detailedTrack = webClient.get()
                    .uri(apiUrl)
                    .retrieve()
                    .bodyToMono(PlaylistController.SoundCloudTrackDto.class)
                    .block();

            if (detailedTrack != null && detailedTrack.duration() != null) {
                return detailedTrack.duration() / 1000;
            }

        } catch (Exception e) {
            System.err.println("Błąd podczas pobierania długości utworu: " + e.getMessage());
        }

        return 0L; // Zwróć 0, jeśli coś poszło nie tak
    }
}