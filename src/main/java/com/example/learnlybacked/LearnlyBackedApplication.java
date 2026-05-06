package com.example.learnlybacked;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
public class LearnlyBackedApplication {

    public static void main(String[] args) {
        SpringApplication.run(LearnlyBackedApplication.class, args);
    }



}

