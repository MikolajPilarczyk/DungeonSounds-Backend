package com.example.learnlybacked.music;

import dev.arbjerg.lavalink.client.LavalinkClient;
import org.springframework.stereotype.Service;


@Service
public class MusicController
{
    private final LavalinkClient client;


    public MusicController(LavalinkClient client)
    {
        this.client = client;
    }


    public void PauseMusic(long guildId)
    {

        System.out.println("Wywolano");
        var link = this.client.getOrCreateLink(guildId);

        link.getPlayer().flatMap(player -> player.setPaused(true))
                .subscribe(
                        updatedPlayer -> System.out.println("Muzyka została zapauzowana."),
                        error -> System.err.println("Błąd: " + error.getMessage())
                );
    }


    public void ResumeMusic(long guildId)
    {
        var link = this.client.getOrCreateLink(guildId);


        link.getPlayer().flatMap(player -> player.setPaused(false))
                .subscribe(
                        updatedPlayer -> System.out.println("Muzyka została odpauzowana."),
                        error -> System.err.println("Błąd: " + error.getMessage())
                );
    }


    public void PlayMusic(long guildId, String trackLink)
    {
        //dodaj dodawanie puszczanie muzyki tutaj
    }

}
