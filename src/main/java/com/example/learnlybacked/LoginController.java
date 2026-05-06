package com.example.learnlybacked;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class LoginController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public record LoginResponse(boolean success, String userName) {}

    @PostMapping("/login")
    public LoginResponse receiveUser(@RequestBody UserLoginDTO user) {
        System.out.println("Odebrano: " + user.getUserEmail());

        String passwordFromDB = "";
        String userName = "";

        List<Object[]> results =  userRepository.findPasswordMatchEmail(user.getUserEmail());
        if (!results.isEmpty()) {
            Object[] row = results.get(0);
            passwordFromDB = (String) row[0];
            userName = (String) row[1];
        }

        String passwordFromFD = user.getPassword();
        if(passwordEncoder.matches(passwordFromFD,passwordFromDB))
        {
            System.out.println("Zalogowane " + userName);
            return new LoginResponse(true,userName);

        }
        else
        {
            System.out.println("Nie poprawny email lub hasło");
            return new LoginResponse(false,"");
        }
    }
}

