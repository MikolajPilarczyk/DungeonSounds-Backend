package com.example.learnlybacked;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class UserProfileController {


    public record UserProfileData(String username, String bio, int likes, int materials,String accountType) {
    }

    public record UsernameToSend(String usernameToFind) {
    }


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPlaylistsSetTableRepository userPlaylistsSetTableRepository;

    @Autowired
    UserLikesRepository userLikesRepository;

    @PostMapping("/user")
    public UserProfileData ReceiveUser(@RequestBody UserLoginDTO user) {

        UserProfileData returnData = userRepository.takeUserProfileDataByUsername(user.getUserNameAndSurname());


        System.out.println("wyslano dane" + returnData);
        return returnData;
    }

    @PostMapping("/find/user")
    public Integer FindUser(@RequestBody UsernameToSend user) {

        if (userRepository.findUserByUsername(user.usernameToFind) == 1)
            return 1;
        else return 0;
    }


/*
    @PostMapping("/user/playlists")
    public List<UserPlaylistsSetTable> Playlists(@RequestBody UsernameToSend user) {
        Long userId = userRepository.getUserIdByUsername(user.usernameToFind);




        return userPlaylistsSetTableRepository.findAllById(userId);
    }*/


    @PostMapping("/user/playlists")
    public List<UserPlaylistsSetTable> Playlists(@RequestBody UsernameToSend user) {
        Long userId = userRepository.getUserIdByUsername(user.usernameToFind);
        UserLoginDTO userEntity = userRepository.getReferenceById(userId);

        List<UserPlaylistsSetTable> returnData = userPlaylistsSetTableRepository.findByUser(userEntity);
        System.out.println("wyslano dane" + returnData.get(0).getTags());

        return returnData;
    }




    @PostMapping("/user/get-liked-playlists")
    public List<UserPlaylistsSetTable> GetLikedPlaylists(@RequestBody UsernameToSend user)
    {
        Long userID = userRepository.getUserIdByUsername(user.usernameToFind);
        List<Long> playlistIds = userLikesRepository.playlistLikedByUser(userID);
        List<UserPlaylistsSetTable> returnData = new ArrayList<>();

        for (int i = 0; i < playlistIds.size(); i++) {
            returnData.add(userPlaylistsSetTableRepository.findById(playlistIds.get(i)).get());
        }

        return returnData;



    }



}
