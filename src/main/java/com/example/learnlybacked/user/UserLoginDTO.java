package com.example.learnlybacked.user;

import com.example.learnlybacked.playlists.UserPlaylistsSetTable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "Users")
public class UserLoginDTO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;
    private String discord_id;




    private String userNameAndSurname;

    private String userEmail;
    private String password;


    private String accountType;
    private String bio;

    @Transient
    private Set<UserPlaylistsSetTable> likedPlaylists;

    private int likes = 0;
    private int materials = 0;
    private boolean agreeToTerms;
}