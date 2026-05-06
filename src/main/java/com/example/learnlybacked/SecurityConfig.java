package com.example.learnlybacked;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // WYMAGANE: Inaczej React dostanie błąd 403 przy POST
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // To wyłącza blokadę i okno logowania
                )
                .formLogin(form -> form.disable()) // Wyłącza formularz ze screena
                .httpBasic(basic -> basic.disable()); // Wyłącza okienko przeglądarki

        return http.build();
    }
}
