package com.example.learnlybacked.playlists;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaylistService {

    private final PlaylistTableRepository playlistRepository;

    public PlaylistService(PlaylistTableRepository playlistRepository) {
        this.playlistRepository = playlistRepository;
    }

    @Transactional
    public SongsTable addSongToPlaylist(PlaylistController.SongToAdd songToAdd) {
        PlaylistTable playlist = playlistRepository.findById(songToAdd.playlistId())
                .orElseThrow(() -> new EntityNotFoundException("Playlista o ID " + songToAdd.playlistId() + " nie została znaleziona"));

        // 2. Stwórz nowy obiekt piosenki
        SongsTable newSong = new SongsTable();
        newSong.setTitle(songToAdd.title());
        newSong.setUrl(songToAdd.url());

        playlist.addSong(newSong);

        playlistRepository.save(playlist);

        return newSong;
    }
}