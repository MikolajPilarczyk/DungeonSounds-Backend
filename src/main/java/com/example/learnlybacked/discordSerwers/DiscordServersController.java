package com.example.learnlybacked.discordSerwers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class DiscordServersController {

    @Autowired
    private DiscordServersRepository discordServersRepository;

    @PostMapping("/get/user/guilds")
    public List<DiscordServersDTO> getUserGuilds(@RequestBody Map<String, String> body) {
        String userIdStr = body.get("userId");

        if (userIdStr == null) {
            return new ArrayList<>();
        }

        Long responseUserId = Long.parseLong(userIdStr.trim());

        return discordServersRepository.findByUserId(responseUserId);
    }
}