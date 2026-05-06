package com.example.learnlybacked;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "UsersPlaylistsSets")
public class UserPlaylistsSetTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title")
    private String title;

    @Column(name = "category")
    private String category;

    @Column(name = "description")
    private String description;

    @Column(name = "tags", columnDefinition = "varchar(255)[]")
    @JsonProperty("tags")
    private List<String> tags;

    @ManyToOne()
    @JoinColumn(name = "userId")
    private UserLoginDTO user;

    @Transient
    private List<UserLoginDTO> usersWhoLiked;

    @JsonManagedReference
    @OneToMany(cascade = CascadeType.ALL , mappedBy = "userPlaylistsSet",fetch = FetchType.EAGER)
    private List<PlaylistTable> playlists = new ArrayList<>();

    public void addPlaylist(PlaylistTable playlistTable) {
        playlists.add(playlistTable);
        playlistTable.setUserPlaylistsSet(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPlaylistsSetTable that = (UserPlaylistsSetTable) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}