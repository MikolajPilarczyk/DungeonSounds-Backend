package com.example.learnlybacked.discordAuth;

import com.example.learnlybacked.discordSerwers.DiscordServersDTO;
import com.example.learnlybacked.discordSerwers.DiscordServersRepository;
import com.example.learnlybacked.user.UserLoginDTO;
import com.example.learnlybacked.user.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DiscordAuth {

    private final UserRepository userRepository;
    private final DiscordServersRepository discordServersRepository;

    @Value("${discord.cliend.id}")  private String clientId;
    @Value("${discord.secret.key}") private String clientSecret;
    @Value("${redirectUri}")        private String redirectUri;
    @Value("${discord.token}")      private String BOT_TOKEN;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    // ----------------------------- DTOs -----------------------------

    @Data
    public static class UserDiscordReturnData {
        public String username, global_name, avatarURL, discordId;
        public Long id;
        public boolean isLogged = false;
    }

    public static class FormData {
        public String code;
    }

    @Data
    public static class GuildsReturnData {
        private String id, name, iconUrl;

        public GuildsReturnData(String id, String name, String icon) {
            this.id = id;
            this.name = name;
            this.iconUrl = icon != null
                    ? "https://cdn.discordapp.com/icons/" + id + "/" + icon + ".png"
                    : null;
        }
    }

    @Data
    private static class DiscordGuildDTO {
        private String id, name, icon;
    }

    // ----------------------------- Discord API calls -----------------------------

    private JsonNode getAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id",     clientId);
        body.add("client_secret", clientSecret);
        body.add("grant_type",    "authorization_code");
        body.add("code",          code);
        body.add("redirect_uri",  redirectUri);

        return restTemplate.postForObject(
                "https://discord.com/api/oauth2/token",
                new HttpEntity<>(body, headers),
                JsonNode.class
        );
    }

    private JsonNode getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        return restTemplate.exchange(
                "https://discord.com/api/users/@me",
                HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class
        ).getBody();
    }

    private DiscordGuildDTO[] getUserGuildList(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        DiscordGuildDTO[] guilds = restTemplate.exchange(
                "https://discord.com/api/users/@me/guilds",
                HttpMethod.GET, new HttpEntity<>(headers), DiscordGuildDTO[].class
        ).getBody();
        return guilds != null ? guilds : new DiscordGuildDTO[0];
    }

    private List<GuildsReturnData> getUserGuilds(String userAccessToken, String discordId) {
        DiscordGuildDTO[] userGuilds = getUserGuildList(userAccessToken);

        HttpHeaders botHeaders = new HttpHeaders();
        botHeaders.set("Authorization", "Bot " + BOT_TOKEN);
        HttpEntity<String> botEntity = new HttpEntity<>(botHeaders);

        List<CompletableFuture<GuildsReturnData>> futures = Arrays.stream(userGuilds)
                .map(guild -> CompletableFuture.supplyAsync(() -> {
                    try {
                        String url = "https://discord.com/api/guilds/" + guild.getId() + "/members/" + discordId.trim();
                        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, botEntity, String.class);
                        if (resp.getStatusCode() == HttpStatus.OK) {
                            return new GuildsReturnData(guild.getId(), guild.getName(), guild.getIcon());
                        }
                    } catch (HttpClientErrorException.NotFound | HttpClientErrorException.Forbidden ignored) {
                    } catch (Exception e) {
                        System.err.println("Błąd sprawdzania serwera " + guild.getId() + ": " + e.getMessage());
                    }
                    return null;
                }, executor))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(g -> g != null)
                .toList();
    }

    // ----------------------------- Endpoint -----------------------------

    @PostMapping("/auth/discord")
    public ResponseEntity<UserDiscordReturnData> receiveUserDiscordData(@RequestBody FormData data) {
        try {
            if (data.code == null || data.code.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            JsonNode tokenData = getAccessToken(data.code);
            if (!tokenData.has("access_token")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            String accessToken = tokenData.get("access_token").asText();

            JsonNode userData = getUserInfo(accessToken);
            String discordId   = userData.get("id").asText();
            String username    = userData.get("username").asText();
            String globalName  = userData.has("global_name") && !userData.get("global_name").isNull()
                    ? userData.get("global_name").asText() : username;
            String avatar      = userData.has("avatar") && !userData.get("avatar").isNull()
                    ? userData.get("avatar").asText() : null;
            String avatarURL   = avatar != null
                    ? "https://cdn.discordapp.com/avatars/" + discordId + "/" + avatar + ".png"
                    : "https://cdn.discordapp.com/embed/avatars/0.png";

            CompletableFuture<List<GuildsReturnData>> guildsFuture =
                    CompletableFuture.supplyAsync(() -> getUserGuilds(accessToken, discordId), executor);

            Long userId;
            if (userRepository.getNumberOfUsersByDiscordId(discordId) == 0) {
                UserLoginDTO userToSave = new UserLoginDTO();
                userToSave.setDiscord_id(discordId);
                userToSave.setUrl(avatarURL);
                userToSave.setUserNameAndSurname(username);
                userRepository.save(userToSave);
                userId = userRepository.getUserIdByDiscord_id(discordId);
            } else {
                userId = userRepository.getUserIdByDiscord_id(discordId);
                userRepository.updateUsername(userId, username);
                userRepository.updateUserUrl(userId, avatarURL);
            }

            Long finalUserId = userId;
            guildsFuture.join().forEach(guild -> {
                DiscordServersDTO dto = new DiscordServersDTO();
                dto.setServerId(guild.getId());
                dto.setServerIconUrl(guild.getIconUrl());
                dto.setServerName(guild.getName());
                dto.setUser(userRepository.getReferenceById(finalUserId));
                discordServersRepository.save(dto);
            });

            UserDiscordReturnData result = new UserDiscordReturnData();
            result.username    = username;
            result.global_name = globalName;
            result.avatarURL   = avatarURL;
            result.discordId   = discordId;
            result.id          = userId;
            result.isLogged    = true;

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}