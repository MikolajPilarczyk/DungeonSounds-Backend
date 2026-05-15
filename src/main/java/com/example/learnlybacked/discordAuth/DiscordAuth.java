package com.example.learnlybacked.discordAuth;

import com.example.learnlybacked.music.MusicController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.stream.Collectors;


@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api")
@Service
public class DiscordAuth {

    @Value("${discord.cliend.id}")
    private String clientId;

    @Value("${discord.secret.key}")
    private String clientSecret;

    @Value("${redirectUri}")
    private String redirectUri;


    public static class FormData {
        public String code;
    }


    @PostMapping("/auth/discord")
    public void ReciveUserDiscordData(@RequestBody FormData data)
    {
        String code = data.code;

        if(code==null || code.isEmpty())
        {
            System.out.println("Invalid code");
        }

    }
    public String getAccessToken(String code) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        // Dane do wysłania w formacie application/x-www-form-urlencoded
        Map<String, String> data = Map.of(
                "client_id", clientId,
                "client_secret", clientSecret,
                "grant_type", "authorization_code",
                "code", code,
                "redirect_uri", redirectUri
        );

        String form = data.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://discord.com/api/v10/oauth2/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Tutaj powinieneś użyć biblioteki JSON (np. Jackson lub Gson), aby wyciągnąć access_token
        return response.body();
    }

}
