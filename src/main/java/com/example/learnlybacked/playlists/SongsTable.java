package com.example.learnlybacked.playlists;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "Songs")
public class SongsTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String url;
    private String duration;

    @ManyToOne
    @JsonBackReference
    @JoinColumn(name = "playlist")
    private PlaylistTable playlist;




}