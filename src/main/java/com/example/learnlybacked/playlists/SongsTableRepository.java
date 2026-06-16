package com.example.learnlybacked.playlists;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SongsTableRepository extends JpaRepository<SongsTable, Long> {
}