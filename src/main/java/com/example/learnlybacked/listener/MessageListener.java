package com.example.learnlybacked.listener;

import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;

@Component
public class MessageListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getMessage().getContentRaw().equals("jeżyr")) {
            event.getChannel().sendMessage("Jebać jeżyra!").queue();
        }

        if (event.getAuthor().isBot()) return;

        String message = event.getMessage().getContentRaw();

        if (message.startsWith("!play ")) {
            String query = message.substring(6).trim();

            if (event.getMember() == null || event.getMember().getVoiceState() == null ||
                    event.getMember().getVoiceState().getChannel() == null) {
                event.getChannel().sendMessage("Musisz znajdować się na kanale głosowym, aby włączyć muzykę!").queue();
                return;
            }

            event.getChannel().sendMessage("Łączenie i odtwarzanie z: " + query).queue();




            net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel voiceChannel = event.getMember().getVoiceState().getChannel().asVoiceChannel();
            net.dv8tion.jda.api.managers.AudioManager audioManager = event.getGuild().getAudioManager();

            long guildId = event.getGuild().getIdLong();
            long channelId = event.getMember().getVoiceState().getChannel().getIdLong();

            //lavalinkClient.getLink(guildId).connect(channelId);

            if (!audioManager.isConnected()) {
                audioManager.openAudioConnection(voiceChannel);
            }


        }
    }



}