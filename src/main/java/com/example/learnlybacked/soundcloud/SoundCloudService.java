package com.example.learnlybacked.soundcloud;

import com.example.learnlybacked.playlists.PlaylistController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class SoundCloudService {

    private static final String RESOLVE_URL = "https://api.soundcloud.com/resolve";

    private final WebClient webClient;
    private final String clientId;

    public SoundCloudService(
            @Value("${soundcloud.client-id}") String clientId
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(RESOLVE_URL)
                .build();
        this.clientId = clientId;
    }

    public PlaylistController.SoundCloudTrackDto getSongDuration(String songUrl) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("url", songUrl)
                        .queryParam("client_id", clientId)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        Mono.error(new RuntimeException(
                                "Błąd API SoundCloud: " + clientResponse.statusCode()))
                )
                .bodyToMono(PlaylistController.SoundCloudTrackDto.class)
                .block();
    }
}