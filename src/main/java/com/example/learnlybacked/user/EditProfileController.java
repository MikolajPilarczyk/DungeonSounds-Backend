package com.example.learnlybacked.user;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class EditProfileController {

    public record ReciveNewUsername(String username,String newUserName){}


    public record ReciveUserBio(Long id,String bio){}

    public record RecivaUserAvatar(String userNameAndSurname,String bio){}

    @Autowired
    UserRepository userRepository;


    @PostMapping("edit-username")
    public String ReciveUserData(@RequestBody ReciveNewUsername user)
    {
        Long userId = userRepository.getUserIdByUsername(user.username);
        int numberOfUsers = userRepository.findUserByUsername(user.newUserName);
        if(numberOfUsers>=1)
        {
            return "Jest już taki urzytkownik o takiej nazwie";
        }
        else
        {
            userRepository.updateUsername(userId,user.newUserName);
            return "Zmieniono nazwe urzytkownika z "+ user.username+ " na " + user.newUserName + userId;
        }
    }


    @PostMapping("edit-bio")
    public String ReciveUserData(@RequestBody ReciveUserBio bio)
    {
        Long userId = bio.id;
        userRepository.updateUserBio(userId,bio.bio);
        return "Zmieniono opis urzytkownika";
    }

    @PostMapping("edit-avatar")
    public RecivaUserAvatar uploadAvatar(@RequestBody RecivaUserAvatar user) {


        return user;
    }


}
