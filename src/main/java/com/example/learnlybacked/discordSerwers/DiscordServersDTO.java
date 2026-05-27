package com.example.learnlybacked.discordSerwers;


import com.example.learnlybacked.user.UserLoginDTO;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "DiscordServers")
public class DiscordServersDTO {
    @Id
    @Column(name = "server_id")
        private String serverId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonBackReference
    private UserLoginDTO user;

    @Column(name = "server_name", nullable = false)
    private String serverName;

    @Column(name = "server_icon_url")
    private String serverIconUrl;
}
