package com.example.learnlybacked.discordAuth;

import com.example.learnlybacked.discordSerwers.DiscordServersDTO;
import com.example.learnlybacked.discordSerwers.DiscordServersRepository;
import com.example.learnlybacked.user.UserLoginDTO;
import com.example.learnlybacked.user.UserRepository;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api")
public class DiscordAuth {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DiscordServersRepository discordServersRepository;

    @Value("${discord.cliend.id}")
    private String clientId;

    @Value("${discord.secret.key}")
    private String clientSecret;

    @Value("${redirectUri}")
    private String redirectUri;

    @Value("${discord.token}")
    private String BOT_TOKEN;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Data
    public static class UserDiscordReturnData {
        public String username;
        public String global_name;
        public String avatarURL;
        public Long id;
        public String discordId;
        public boolean isLogged = false;
    }

    public static class FormData {
        public String code;
    }

    @Data
    public static class GuildsReturnData {
        private String id;
        private String name;
        private String iconUrl;

        public GuildsReturnData(String id, String name, String icon) {
            this.id = id;
            this.name = name;
            this.iconUrl = (icon != null)
                    ? "https://cdn.discordapp.com/icons/" + id + "/" + icon + ".png"
                    : null;
        }
    }

    @Data
    private static class DiscordGuildDTO {
        private String id;
        private String name;
        private String icon;
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
                .POST(HttpRequest.BodyPublishers.ofString(form))
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

    public List<GuildsReturnData> getUserGuilds(String userAccessToken, String discordId) throws Exception {
        List<GuildsReturnData> sharedGuilds = new ArrayList<>();

        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.set("Authorization", "Bearer " + userAccessToken);
        HttpEntity<String> userEntity = new HttpEntity<>(userHeaders);

        DiscordGuildDTO[] userGuilds;
        try {
            String guildsUrl = "https://discord.com/api/users/@me/guilds";
            ResponseEntity<DiscordGuildDTO[]> response = restTemplate.exchange(
                    guildsUrl, HttpMethod.GET, userEntity, DiscordGuildDTO[].class
            );
            userGuilds = response.getBody();
        } catch (Exception e) {
            throw new Exception("Błąd podczas pobierania serwerów użytkownika z Discord API: " + e.getMessage());
        }

        HttpHeaders botHeaders = new HttpHeaders();
        botHeaders.set("Authorization", "Bot " + BOT_TOKEN);
        HttpEntity<String> botEntity = new HttpEntity<>(botHeaders);

        if (userGuilds != null) {
            for (DiscordGuildDTO guild : userGuilds) {
                String memberUrl = "https://discord.com/api/guilds/" + guild.getId() + "/members/" + discordId.trim();
                try {
                    ResponseEntity<String> memberResponse = restTemplate.exchange(
                            memberUrl, HttpMethod.GET, botEntity, String.class
                    );

                    if (memberResponse.getStatusCode() == HttpStatus.OK) {
                        sharedGuilds.add(new GuildsReturnData(guild.getId(), guild.getName(), guild.getIcon()));
                    }
                } catch (HttpClientErrorException.NotFound | HttpClientErrorException.Forbidden e) {
                    // Ignorujemy jeśli bot nie ma dostępu lub użytkownika nie ma na serwerze
                } catch (Exception e) {
                    // Logowanie innych błędów, aby nie wywalać całej aplikacji
                    System.err.println("Błąd sprawdzania serwera " + guild.getId() + ": " + e.getMessage());
                }
            }
        }
        return sharedGuilds;
    }
    @PostMapping("/auth/discord")
    public ResponseEntity<UserDiscordReturnData> receiveUserDiscordData(@RequestBody FormData data) {
        try {
            String code = data.code;
            if (code == null || code.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            String accessTokenResponse = getAccessToken(code);
            JsonNode dataFromCode = objectMapper.readTree(accessTokenResponse);

            if (!dataFromCode.has("access_token")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            String accessToken = dataFromCode.get("access_token").asText();

            String userDataResponse = getUserInfo(accessToken);
            JsonNode userDataFromResponse = objectMapper.readTree(userDataResponse);

            String discordId = userDataFromResponse.get("id").asText();
            int numberOfUsers = userRepository.getNumberOfUsersByDiscordId(discordId);

            String usernameFromResponse = userDataFromResponse.get("username").asText();
            String globalNameFromResponse = userDataFromResponse.has("global_name") && !userDataFromResponse.get("global_name").isNull()
                    ? userDataFromResponse.get("global_name").asText() : usernameFromResponse;

            // Pobieranie i zabezpieczenie awatara (jeśli użytkownik nie ma awatara, ustawia domyślny)
            String avatar = userDataFromResponse.has("avatar") && !userDataFromResponse.get("avatar").isNull()
                    ? userDataFromResponse.get("avatar").asText() : null;
            String avatarURL = avatar != null
                    ? "https://cdn.discordapp.com/avatars/" + discordId + "/" + avatar + ".png"
                    : "https://cdn.discordapp.com/embed/avatars/0.png";

            UserDiscordReturnData userDiscordReturnData = new UserDiscordReturnData();
            userDiscordReturnData.username = usernameFromResponse;
            userDiscordReturnData.global_name = globalNameFromResponse;
            userDiscordReturnData.isLogged = true;
            userDiscordReturnData.discordId = discordId;
            userDiscordReturnData.avatarURL = avatarURL;




            Long userId;

            if (numberOfUsers == 0) {
                // PROCES REJESTRACJI (Nowy użytkownik)
                System.out.println("Rejestracja nowego użytkownika: " + usernameFromResponse);
                UserLoginDTO userDataToSave = new UserLoginDTO();
                userDataToSave.setDiscord_id(discordId);
                userDataToSave.setUrl(avatarURL);
                userDataToSave.setUserNameAndSurname(usernameFromResponse);

                userRepository.save(userDataToSave);
                userId = userRepository.getUserIdByDiscord_id(discordId);
            } else {
                // PROCES LOGOWANIA (Istniejący użytkownik)
                userId = userRepository.getUserIdByDiscord_id(discordId);

                // WYMUSZENIE AKTUALIZACJI: Przy każdym logowaniu wysyłamy najnowsze dane z Discorda do bazy
                System.out.println("Aktualizacja danych dla istniejącego użytkownika o ID: " + userId);
                userRepository.updateUsername(userId, usernameFromResponse);
                userRepository.updateUserUrl(userId, avatarURL);
            }

            userDiscordReturnData.id = userId;

            // OBSŁUGA SERWERÓW
            List<GuildsReturnData> discordGuilds = getUserGuilds(accessToken, discordId);

            // Opcjonalnie: Jeśli chcesz uniknąć duplikowania serwerów w bazie danych przy wielokrotnym logowaniu,
            // odkomentuj poniższą linię (wymaga dodania metody deleteByUserId w discordServersRepository)
            // discordServersRepository.deleteByUserId(userId);

            for (GuildsReturnData guildData : discordGuilds) {
                DiscordServersDTO dataToSave = new DiscordServersDTO();
                dataToSave.setServerId(guildData.getId());
                dataToSave.setServerIconUrl(guildData.getIconUrl());
                dataToSave.setServerName(guildData.getName());
                dataToSave.setUser(userRepository.getReferenceById(userId));

                discordServersRepository.save(dataToSave);
            }

            return ResponseEntity.ok(userDiscordReturnData);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}