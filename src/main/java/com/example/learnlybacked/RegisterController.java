package com.example.learnlybacked;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class RegisterController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;




    @PostMapping("/register")
    public String receiveUser(@RequestBody UserLoginDTO  user) {
        System.out.println("Odebrano: " + user.getUserEmail());
        if(userRepository.findByCustomEmail(user.getUserEmail()) ==0 && userRepository.findUserByUsername(user.getUserNameAndSurname())==0)
        {
            String rawPassword = user.getPassword();
            String hashedPassword = passwordEncoder.encode(rawPassword);
            user.setPassword(hashedPassword);
            userRepository.save(user);
        }
        else
        {

            System.out.println("Jest już taki user w bazie dancyh");
            return "Urzytkownik z takim adresem emailem lub nazwą urzytkownika już istnieje";

        }



        return "Zarejestrowanie zakonończone pomyślnie";
    }


}