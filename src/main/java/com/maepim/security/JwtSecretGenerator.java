package com.maepim.security;

import io.jsonwebtoken.security.Keys;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

public class JwtSecretGenerator {

    public static void main(String[] args) {
        generateSecretAndSaveToFile();
    }

    public static void generateSecretAndSaveToFile() {
        // Generate a 32-byte (256-bit) secret key
        SecureRandom random = new SecureRandom();
        byte[] secretBytes = new byte[32];
        random.nextBytes(secretBytes);

        // Encode the secret key in Base64
        String secret = Base64.getEncoder().encodeToString(secretBytes);

        // Save the secret to a file named jwt.secret in the project root
        try (FileOutputStream fos = new FileOutputStream("jwt.secret")) {
            fos.write(secret.getBytes());
            System.out.println("JWT secret generated and saved to jwt.secret");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}