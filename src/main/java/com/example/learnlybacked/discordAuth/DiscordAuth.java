package com.example.learnlybacked.discordAuth;

import com.example.learnlybacked.music.MusicController;
import com.example.learnlybacked.user.UserLoginDTO;
import com.example.learnlybacked.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    UserRepository userRepository;

    @Value("${discord.cliend.id}")
    private String clientId;

    @Value("${discord.secret.key}")
    private String clientSecret;

    @Value("${redirectUri}")
    private String redirectUri;

    public static class UserDiscordReturnData
    {
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
    public UserDiscordReturnData ReciveUserDiscordData(@RequestBody FormData data) throws Exception {
        String code = data.code;
        System.out.println("codeIs: " + code);

        if(code==null || code.isEmpty())
        {
            System.out.println("Invalid code");
            return null;
        }
        else {
            String accesToken = getAccessToken(code);
            System.out.println("Access token: " + accesToken);



            JsonNode dataFromCode = new ObjectMapper().readTree(accesToken);


            String accessToken = dataFromCode.get("access_token").asText();

            //Take it if you will need it
            //
            //String refreshToken = dataFromCode.get("refresh_token").asText();
            //String tokenType = dataFromCode.get("token_type").asText();
            //String expiresIn = dataFromCode.get("expires_in").asText();

            String userDataRespone = getUserInfo(accessToken);
            System.out.println("userDataRespone: " + userDataRespone);
            JsonNode userDataFromRespone = new ObjectMapper().readTree(userDataRespone);




            String discord_id = userDataFromRespone.get("id").asString();
            int numberOfUsers = userRepository.getNumberOfUsersByDiscordId(discord_id);

            if(numberOfUsers==0)
            {
                // User Register Procces
                //dodaj usera
                System.out.println("No users found");
                UserLoginDTO userDataToSave = new UserLoginDTO();
                userDataToSave.setDiscord_id(discord_id);

                // tutaj ważne dodaj skrypt który zmienie tą nazwe urzytkownika w momencie gdy skrypt wykryje że różni się ona od tej jaką ma sie na discordzie
                userDataToSave.setUserNameAndSurname(userDataFromRespone.get("username").asText());


                userRepository.save(userDataToSave);

                UserDiscordReturnData userDiscordReturnData = new UserDiscordReturnData();

                userDiscordReturnData.username = userDataFromRespone.get("username").asText();
                userDiscordReturnData.global_name = userDataFromRespone.get("global_name").asText();
                userDiscordReturnData.isLogged = true;
                userDiscordReturnData.discordId = discord_id;

                String avatar = userDataFromRespone.get("avatar").asText();
                String avatarURL = "https://cdn.discordapp.com/avatars/"+discord_id+"/"+avatar+".png";

                userDiscordReturnData.avatarURL = avatarURL;

                Long userId = userRepository.getUserIdByDiscord_id(discord_id);
                userDiscordReturnData.id = userId;


                System.out.println("regged in" + userDiscordReturnData);


                return userDiscordReturnData;

            }
            else{




                UserDiscordReturnData userDiscordReturnData = new UserDiscordReturnData();
                userDiscordReturnData.username = userDataFromRespone.get("username").asText();
                userDiscordReturnData.global_name = userDataFromRespone.get("global_name").asText();
                userDiscordReturnData.isLogged = true;
                userDiscordReturnData.discordId = discord_id;

                String avatar = userDataFromRespone.get("avatar").asText();
                String avatarURL = "https://cdn.discordapp.com/avatars/"+discord_id+"/"+avatar+".png";

                userDiscordReturnData.avatarURL = avatarURL;


                Long userId = userRepository.getUserIdByDiscord_id(discord_id);
                userDiscordReturnData.id = userId;

                System.out.println("logged in" + userDiscordReturnData);

                return userDiscordReturnData;

            }




        }

    }


}
