package com.example.learnlybacked;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface UserLikesRepository extends JpaRepository<UserPlaylistsSetTable, Long> {


    // Dodaje polubienie playlisty
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO user_liked_playlists (user_id, playlist_id) VALUES (:userId, :playlistId) ON CONFLICT DO NOTHING", nativeQuery = true)
    void likePlaylist(@Param("userId") Long userId, @Param("playlistId") Long playlistId);

    // Usuwa polubienie playlisty
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM user_liked_playlists WHERE user_id = :userId AND playlist_id = :playlistId", nativeQuery = true)
    void disLikePlaylist(@Param("userId") Long userId, @Param("playlistId") Long playlistId);

    // Sprawdza, czy polubienie istnieje
    @Query(value = "SELECT COUNT(*) > 0 FROM user_liked_playlists WHERE user_id = :userId AND playlist_id = :playlistId", nativeQuery = true)
    boolean isLiked(@Param("userId") Long userId, @Param("playlistId") Long playlistId);

    //Pobiera polubione playliste

    @Query(value = "SELECT playlist_id FROM user_liked_playlists WHERE user_id = :userId", nativeQuery = true)
    List<Long> playlistLikedByUser(@Param("userId") Long userId);

}