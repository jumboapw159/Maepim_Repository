package com.maepim.security.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.annotation.PostConstruct; // Import PostConstruct
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory

import java.util.Collections;

@Configuration
public class GoogleAuthConfig {

    private static final Logger logger = LoggerFactory.getLogger(GoogleAuthConfig.class); // Initialize Logger

    @Value("${maepim.app.googleClientId}")
    private String googleClientId;

    @PostConstruct
    public void init() {
        logger.info("Google Client ID loaded: {}", googleClientId);
    }

    @Bean
    public GoogleIdTokenVerifier googleIdTokenVerifier() {
        return new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }
}