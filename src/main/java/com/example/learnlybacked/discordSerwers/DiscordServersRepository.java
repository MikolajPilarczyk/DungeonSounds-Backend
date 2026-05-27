package com.example.learnlybacked.discordSerwers;

import com.example.learnlybacked.user.UserLoginDTO;
import com.example.learnlybacked.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscordServersRepository extends JpaRepository<DiscordServersDTO, Long> {


    @Query("SELECT d FROM DiscordServersDTO d WHERE d.user.id = :userId")
    List<DiscordServersDTO> findByUserId(@Param("userId") Long userId);
}
