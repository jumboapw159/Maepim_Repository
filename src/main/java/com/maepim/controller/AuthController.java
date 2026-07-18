package com.maepim.controller;

import com.maepim.dto.request.FacebookLoginRequest; // New import
import com.maepim.dto.request.ForgotPasswordRequest;
import com.maepim.dto.request.GoogleLoginRequest;
import com.maepim.dto.request.LoginRequest;
import com.maepim.dto.request.RefreshTokenRequest;
import com.maepim.dto.request.ResetPasswordRequest;
import com.maepim.dto.request.SignupRequest;
import com.maepim.dto.request.VerifyOtpRequest;
import com.maepim.dto.response.JwtResponse;
import com.maepim.dto.response.MessageResponse;
import com.maepim.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication and management APIs")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Operation(summary = "User Sign In", description = "Authenticates a user with username and password, returning JWT and refresh tokens.")
    @ApiResponse(responseCode = "200", description = "User authenticated successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = JwtResponse.class)))
    @ApiResponse(responseCode = "401", description = "Authentication failed (e.g., invalid credentials)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse jwtResponse = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(jwtResponse);
    }

    @Operation(summary = "User Sign Up", description = "Registers a new user with provided details.")
    @ApiResponse(responseCode = "200", description = "User registered successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "400", description = "Bad request (e.g., username/email already taken, validation errors)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        try {
            authService.registerUser(signUpRequest);
            return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @Operation(summary = "User Sign Out", description = "Logs out the current user by invalidating their refresh token.")
    @ApiResponse(responseCode = "200", description = "Log out successful",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "400", description = "Bad request (e.g., no user authenticated)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    @PostMapping("/logout") // Renamed from /signout
    public ResponseEntity<?> logoutUser() {
        try {
            authService.logoutUser();
            return ResponseEntity.ok(new MessageResponse("Log out successful!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @Operation(summary = "Refresh JWT Token", description = "Refreshes an expired JWT using a valid refresh token.")
    @ApiResponse(responseCode = "200", description = "JWT refreshed successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = JwtResponse.class)))
    @ApiResponse(responseCode = "400", description = "Bad request (e.g., invalid/expired refresh token)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        try {
            JwtResponse jwtResponse = authService.refreshToken(refreshTokenRequest.getRefreshToken());
            return ResponseEntity.ok(jwtResponse);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @Operation(summary = "Forgot Password", description = "Initiates password reset by sending an OTP to the user's email. Returns a generic success message to prevent user enumeration.")
    @ApiResponse(responseCode = "200", description = "OTP sent to your email (if registered and active)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "400", description = "Bad request (e.g., account inactive)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        try {
            authService.forgotPassword(forgotPasswordRequest.getEmail());
            return ResponseEntity.ok(new MessageResponse("OTP sent to your email."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @Operation(summary = "Verify OTP", description = "Verifies the One-Time Password sent to the user's email for password reset.")
    @ApiResponse(responseCode = "200", description = "OTP verified successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "400", description = "Bad request (e.g., invalid/expired OTP, email not found)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest verifyOtpRequest) {
        try {
            authService.verifyOtp(verifyOtpRequest.getEmail(), verifyOtpRequest.getOtp());
            return ResponseEntity.ok(new MessageResponse("OTP verified successfully."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @Operation(summary = "Reset Password", description = "Resets user password using a valid OTP and email.")
    @ApiResponse(responseCode = "200", description = "Password reset successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "400", description = "Bad request (e.g., invalid/expired OTP, email not found)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {
        try {
            authService.resetPassword(resetPasswordRequest.getEmail(), resetPasswordRequest.getOtp(), resetPasswordRequest.getNewPassword());
            return ResponseEntity.ok(new MessageResponse("Password has been reset successfully."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @Operation(summary = "Google Sign In", description = "Authenticates user via Google ID Token, registering if new, and returns JWT and refresh tokens.")
    @ApiResponse(responseCode = "200", description = "User authenticated successfully via Google",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = JwtResponse.class)))
    @ApiResponse(responseCode = "400", description = "Bad request (e.g., invalid Google ID Token, account inactive)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    @PostMapping("/google") // Renamed from /google-signin
    public ResponseEntity<?> googleSignIn(@Valid @RequestBody GoogleLoginRequest googleLoginRequest) {
        try {
            JwtResponse jwtResponse = authService.googleSignIn(googleLoginRequest.getIdToken());
            return ResponseEntity.ok(jwtResponse);
        } catch (GeneralSecurityException | IOException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error verifying Google ID Token: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @Operation(summary = "Facebook Sign In", description = "Authenticates user via Facebook Access Token, registering if new, and returns JWT and refresh tokens.")
    @ApiResponse(responseCode = "200", description = "User authenticated successfully via Facebook",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = JwtResponse.class)))
    @ApiResponse(responseCode = "400", description = "Bad request (e.g., invalid Facebook Access Token, account inactive)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    @PostMapping("/facebook")
    public ResponseEntity<?> facebookSignIn(@Valid @RequestBody FacebookLoginRequest facebookLoginRequest) {
        try {
            JwtResponse jwtResponse = authService.facebookSignIn(facebookLoginRequest.getAccessToken());
            return ResponseEntity.ok(jwtResponse);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
}