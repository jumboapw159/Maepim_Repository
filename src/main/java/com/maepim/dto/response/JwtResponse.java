package com.maepim.dto.response;

import com.maepim.entity.UserStatus; // Import UserStatus
import java.util.List;
import lombok.Getter; // Import Getter
import lombok.Setter; // Import Setter

@Getter
@Setter
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private String refreshToken;
    private Long id;
    private String username;
    private String email;
    private String firstName; // New field
    private String lastName;  // New field
    private String phone;     // New field
    private UserStatus status; // New field
    private List<String> roles;

    public JwtResponse(String accessToken, String refreshToken, Long id, String username, String email, String firstName, String lastName, String phone, UserStatus status, List<String> roles) {
        this.token = accessToken;
        this.refreshToken = refreshToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.firstName = firstName; // Initialize new field
        this.lastName = lastName;   // Initialize new field
        this.phone = phone;         // Initialize new field
        this.status = status;       // Initialize new field
        this.roles = roles;
    }
}