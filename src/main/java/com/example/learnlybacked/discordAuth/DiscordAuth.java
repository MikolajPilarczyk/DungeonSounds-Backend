package com.example.learnlybacked.discordAuth;

import com.example.learnlybacked.music.MusicController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

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

    public static class UserData
    {
        public String username;
        public String avatar;
        public String global_name;
    }

    public static class FormData {
        public String code;
    }

    public String getAccessToken(String code) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

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
                .uri(URI.create("https://discord.com/api/oauth2/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    public String getUserInfo(String accessToken) throws Exception {

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://discord.com/api/users/@me"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();


    }


    @PostMapping("/auth/discord")
    public void ReciveUserDiscordData(@RequestBody FormData data) throws Exception {
        String code = data.code;
        System.out.println("codeIs: " + code);

        if(code==null || code.isEmpty())
        {
            System.out.println("Invalid code");
        }
        else {
            String accesToken = getAccessToken(code);
            System.out.println("Access token: " + accesToken);

            JsonNode jsonNode = new ObjectMapper().readTree(accesToken);


            String accessToken = jsonNode.get("access_token").asText();
            String refreshToken = jsonNode.get("refresh_token").asText();
            String tokenType = jsonNode.get("token_type").asText();
            String expiresIn = jsonNode.get("expires_in").asText();

            String userDataRespone = getUserInfo(accessToken);

            System.out.println("userDataRespone: " + userDataRespone);

            // Teraz dodaj tu rejestracie/logowanie wyszukaj po tym czy jest już osoba z tym discord id


        }

    }


}
