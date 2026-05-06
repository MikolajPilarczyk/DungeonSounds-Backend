package com.example.learnlybacked.disocrd.config;

import dev.arbjerg.lavalink.client.Helpers;
import dev.arbjerg.lavalink.client.LavalinkClient;
import dev.arbjerg.lavalink.client.LavalinkNode;
import dev.arbjerg.lavalink.client.NodeOptions;
import dev.arbjerg.lavalink.client.event.StatsEvent;
import dev.arbjerg.lavalink.client.event.TrackStartEvent;
import dev.arbjerg.lavalink.client.loadbalancing.builtin.VoiceRegionPenaltyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

@Configuration
public class LavalinkConfig {

    private static final Logger log = LoggerFactory.getLogger(LavalinkConfig.class);

    @Value("${discord.token}")
    private String token;

    @Value("${lavalink.node.password}")
    private String nodePassword;

    @Bean
    public LavalinkClient lavalinkClient() throws InterruptedException {
        long userId = Helpers.getUserIdFromToken(token);
        LavalinkClient client = new LavalinkClient(userId);
        client.getLoadBalancer().addPenaltyProvider(new VoiceRegionPenaltyProvider());

        registerLavalinkNodes(client);
        registerLavalinkListeners(client);

        return client;
    }

    private void registerLavalinkNodes(LavalinkClient client) {
        String nodeName = "Lavalink-Server";
        String uri = "ws://127.0.0.1:2333";

        log.info("Registering Lavalink node '{}' at {}", nodeName, uri);

        // Utwórz i dodaj węzeł
        LavalinkNode node = client.addNode(new NodeOptions.Builder()
                .setName(nodeName)
                .setServerUri(URI.create(uri))
                .setPassword(nodePassword)
                .setHttpTimeout(5000L)
                .build()
        );
    }

    private void registerLavalinkListeners(LavalinkClient client) {
        client.on(dev.arbjerg.lavalink.client.event.ReadyEvent.class).subscribe((event) -> {
            final LavalinkNode node = event.getNode();
            log.info("Node '{}' is ready, session id is '{}'!", node.getName(), event.getSessionId());
        });

        client.on(StatsEvent.class).subscribe((event) -> {
            final LavalinkNode node = event.getNode();
            log.debug("Node '{}' has stats, current players: {}/{}", node.getName(), event.getPlayingPlayers(), event.getPlayers());
        });

        client.on(TrackStartEvent.class).subscribe((event) -> {
            log.info("Track started: {}", event.getTrack().getInfo().getTitle());
        });
    }
}