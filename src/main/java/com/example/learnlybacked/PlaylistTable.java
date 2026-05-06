package com.example.learnlybacked;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name="Playlists")
public class PlaylistTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "set_id")
    private UserPlaylistsSetTable userPlaylistsSet;

    @JsonManagedReference
    @OneToMany(cascade = CascadeType.ALL , mappedBy = "playlist")
    private List<SongsTable> songs = new ArrayList<>();
    public void addSong(SongsTable songsTable) {
        songs.add(songsTable);
        songsTable.setPlaylist(this);
    }

}
