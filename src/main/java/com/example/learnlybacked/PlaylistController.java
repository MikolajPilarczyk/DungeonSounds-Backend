package com.example.learnlybacked;

import jakarta.transaction.Transactional;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;


@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api")
public class PlaylistController
{
    public static class FormDataSongs
    {
        public String title;
        public String url;
    }
    public static class FormDataUserPlaylists
    {
        public String title;
        public List<FormDataSongs> songs;
    }
    public static class FormDataPlaylistSet
    {
        public String username;
        public String title;
        public String category;
        public String description;
        public List<String> tags;
        public List<FormDataUserPlaylists> playlists;
    }

    record PlaylistId(Long playlistId) { }



    @Autowired
    UserRepository userRepository;

    @Autowired
    private UserPlaylistsSetTableRepository userPlaylistsSetTableRepository;



    @Transactional
    @PostMapping("/upload")
    public String UploadPlaylistSet(@RequestBody FormDataPlaylistSet data)
    {
        UserPlaylistsSetTable dataToSave = new UserPlaylistsSetTable();
        Long userID = userRepository.getUserIdByUsername(data.username);
        if (userID == null) {
            System.out.println(data.username);
            return "Błąd: Nie znaleziono użytkownika o nazwie: " + data.username;
        }

        dataToSave.setTitle(data.title);
        dataToSave.setCategory(data.category);
        dataToSave.setTags(data.tags);
        dataToSave.setDescription(data.description);
        dataToSave.setUser(userRepository.getReferenceById(userID));

        for (int i = 0; i < data.playlists.size(); i++) {
            //Current playlists
            PlaylistTable playlistToSave = new PlaylistTable();
            playlistToSave.setTitle(data.playlists.get(i).title);

            for (int j = 0; j < data.playlists.get(i).songs.size(); j++) {
                //Current Song
                SongsTable song = new SongsTable();
                song.setTitle(data.playlists.get(i).songs.get(j).title);
                song.setUrl(data.playlists.get(i).songs.get(j).url);

                playlistToSave.addSong(song);
            }

            dataToSave.addPlaylist(playlistToSave);
        }

        userPlaylistsSetTableRepository.save(dataToSave);


        return "Zapisano Very Good";
    }

    @PostMapping("/get-playlist")
    public List<UserPlaylistsSetTable> RecivePlaylistSet(@RequestBody PlaylistId data)
    {

        System.out.println("wyslano "+userPlaylistsSetTableRepository.findByPlaylistByID(data.playlistId));

        return userPlaylistsSetTableRepository.findByPlaylistByID(data.playlistId);
    }


    @Getter
    @Setter
    public static class UserDataToLikePlaylist
    {
        String username;
        Long playlistId;
    }


    @Autowired UserLikesRepository userLikesRepository;

    @Transactional
    @PostMapping("/like-playlist")
    public String LikePlaylist(@RequestBody UserDataToLikePlaylist data)
    {
        Long userID = userRepository.getUserIdByUsername(data.getUsername());

        userRepository.giveLike(userID);
        userLikesRepository.likePlaylist(userID, data.getPlaylistId());
        return "Polubione pomyślnie";
    }

    @Transactional
    @PostMapping("/unlike-playlist")
    public String UnlikePlaylist(@RequestBody UserDataToLikePlaylist data)
    {

        Long userID = userRepository.getUserIdByUsername(data.getUsername());


        userRepository.takeAwayLike(userID);
        userLikesRepository.disLikePlaylist(userID, data.getPlaylistId());
        return "Usunięto polubienie pomyślnie";

    }


    @PostMapping("/isLiked")
    public boolean IsLiked(@RequestBody UserDataToLikePlaylist data)
    {
        Long userID = userRepository.getUserIdByUsername(data.getUsername());


        return userLikesRepository.isLiked(userID, data.getPlaylistId());

    }




}
