package com.maepim.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.maepim.config.AppConfig; // Import AppConfig
import com.maepim.dto.request.LoginRequest;
import com.maepim.dto.request.SignupRequest;
import com.maepim.dto.response.JwtResponse;
import com.maepim.entity.*;
import com.maepim.repository.AddressRepository;
import com.maepim.repository.RoleRepository;
import com.maepim.repository.UserRepository;
import com.maepim.security.jwt.JwtService;
import com.maepim.security.service.CustomUserDetailsService;
import com.maepim.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate; // Import RestTemplate

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private GoogleIdTokenVerifier googleIdTokenVerifier;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private RestTemplate restTemplate; // Inject RestTemplate

    @Value("${maepim.app.googleClientId}")
    private String googleClientId;

    @Value("${maepim.app.facebookAppId}")
    private String facebookAppId;

    @Value("${maepim.app.facebookAppSecret}")
    private String facebookAppSecret;

    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found after authentication.")); // Should not happen

        String refreshToken = refreshTokenService.createRefreshToken(userDetails.getId()).getToken();

        return generateJwtResponse(userDetails, refreshToken, user);
    }

    public void logoutUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            Long userId = userDetails.getId();
            refreshTokenService.deleteByUserId(userId);
        } else {
            throw new RuntimeException("No user is currently authenticated.");
        }
    }

    @Transactional
    public void registerUser(SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new RuntimeException("Error: Username is already taken!");
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new RuntimeException("Error: Email is already in use!");
        }

        // Create new user's account
        User user = new User(signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()));

        Set<String> strRoles = signUpRequest.getRole();
        Set<Role> roles = new HashSet<>();
        if (strRoles == null) {
            Role userRole = roleRepository.findByRoleName("ROLE_CUSTOMER")
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin":
                        Role adminRole = roleRepository.findByRoleName("ROLE_ADMIN")
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(adminRole);

                        break;
                    case "super_admin":
                        Role superAdminRole = roleRepository.findByRoleName("ROLE_SUPER_ADMIN")
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(superAdminRole);
                        break;
                    default:
                        Role userRole = roleRepository.findByRoleName("ROLE_CUSTOMER")
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(userRole);
                }
            });
        }

        // Set user properties
        user.setRoles(roles);
        user.setStatus(signUpRequest.getStatus());
        user.setPhone(signUpRequest.getPhone());
        user.setFirstName(signUpRequest.getFirstName());
        user.setLastName(signUpRequest.getLastName());
        userRepository.save(user);

        Address address = new Address();

        address.setUser(user);
        address.setAddress(signUpRequest.getAddress().getAddress());
        address.setSubdistrict(signUpRequest.getAddress().getSubdistrict());
        address.setDistrict(signUpRequest.getAddress().getDistrict());
        address.setProvince(signUpRequest.getAddress().getProvince());
        address.setPostalCode(signUpRequest.getAddress().getPostalCode());
        addressRepository.save(address);

    }

    @Transactional
    public JwtResponse googleSignIn(String idToken) throws GeneralSecurityException, IOException {
        logger.info("Attempting Google Sign-In with ID Token.");
        GoogleIdToken googleIdToken = googleIdTokenVerifier.verify(idToken);
        if (googleIdToken == null) {
            logger.warn("Invalid Google ID Token received.");
            throw new RuntimeException("Invalid Google ID Token.");
        }

        GoogleIdToken.Payload payload = googleIdToken.getPayload();
        String email = payload.getEmail();
        String firstName = (String) payload.get("given_name");
        String lastName = (String) payload.get("family_name");
        String username = email; // Use email as username for Google sign-in

        logger.info("Google ID Token verified for email: {}", email);

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isEmpty()) {
            logger.info("User with email {} not found. Registering new user.", email);
            // Register new user
            user = new User(username, email, encoder.encode(UUID.randomUUID().toString())); // Generate random password
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setStatus(UserStatus.ACTIVE); // Set default status
            user.setEmailVerified(true); // Google verified email

            Set<Role> roles = new HashSet<>();
            Role userRole = roleRepository.findByRoleName("ROLE_CUSTOMER")
                    .orElseThrow(() -> {
                        logger.error("Error: ROLE_CUSTOMER not found during Google sign-in registration.");
                        return new RuntimeException("Error: Role is not found.");
                    });
            roles.add(userRole);
            user.setRoles(roles);
            userRepository.save(user);
            logger.info("New user {} registered successfully via Google.", email);
        } else {
            user = userOptional.get();
            logger.info("User with email {} found. Logging in.", email);
            // Update user details from Google payload
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmailVerified(true); // Google verified email
            userRepository.save(user); // Save updated user
            logger.debug("User {} details updated from Google payload.", email);

            // Ensure user is active
            if (user.getStatus() != UserStatus.ACTIVE) {
                logger.warn("Attempted Google sign-in for inactive account: {}", email);
                throw new RuntimeException("Account is inactive. Please contact support.");
            }
        }

        // Load UserDetails for the identified/created user
        UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername(user.getUsername());
        logger.debug("UserDetails loaded for user: {}", user.getUsername());

        // Create an authenticated token directly
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        String refreshToken = refreshTokenService.createRefreshToken(userDetails.getId()).getToken();
        logger.info("Google Sign-In successful for user: {}", email);

        return generateJwtResponse(userDetails, refreshToken, user);
    }

    @Transactional
    public JwtResponse facebookSignIn(String accessToken) {
        logger.info("Attempting Facebook Sign-In with Access Token.");

        // 1. Debug the access token with Facebook Graph API
        String debugTokenUrl = String.format("https://graph.facebook.com/debug_token?input_token=%s&access_token=%s|%s",
                accessToken, facebookAppId, facebookAppSecret);
        Map<String, Object> debugResponse = restTemplate.getForObject(debugTokenUrl, Map.class);

        if (debugResponse == null || !((Map<String, Object>) debugResponse.get("data")).get("is_valid").equals(true)) {
            logger.warn("Invalid Facebook Access Token received.");
            throw new RuntimeException("Invalid Facebook Access Token.");
        }

        // 2. Get user profile details
        String userProfileUrl = String.format("https://graph.facebook.com/me?fields=id,email,first_name,last_name&access_token=%s", accessToken);
        Map<String, Object> userProfile = restTemplate.getForObject(userProfileUrl, Map.class);

        if (userProfile == null || !userProfile.containsKey("email")) {
            logger.warn("Could not retrieve email from Facebook profile. User profile: {}", userProfile);
            throw new RuntimeException("Could not retrieve email from Facebook profile. Ensure 'email' permission is granted.");
        }

        String email = (String) userProfile.get("email");
        String firstName = (String) userProfile.get("first_name");
        String lastName = (String) userProfile.get("last_name");
        String username = email; // Use email as username for Facebook sign-in

        logger.info("Facebook Access Token verified for email: {}", email);

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isEmpty()) {
            logger.info("User with email {} not found. Registering new user via Facebook.", email);
            // Register new user
            user = new User(username, email, encoder.encode(UUID.randomUUID().toString())); // Generate random password
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setStatus(UserStatus.ACTIVE); // Set default status
            user.setEmailVerified(true); // Facebook verified email

            Set<Role> roles = new HashSet<>();
            Role userRole = roleRepository.findByRoleName("ROLE_CUSTOMER")
                    .orElseThrow(() -> {
                        logger.error("Error: ROLE_CUSTOMER not found during Facebook sign-in registration.");
                        return new RuntimeException("Error: Role is not found.");
                    });
            roles.add(userRole);
            user.setRoles(roles);
            userRepository.save(user);
            logger.info("New user {} registered successfully via Facebook.", email);
        } else {
            user = userOptional.get();
            logger.info("User with email {} found. Logging in via Facebook.", email);
            // Update user details from Facebook payload
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmailVerified(true); // Facebook verified email
            userRepository.save(user); // Save updated user
            logger.debug("User {} details updated from Facebook payload.", email);

            // Ensure user is active
            if (user.getStatus() != UserStatus.ACTIVE) {
                logger.warn("Attempted Facebook sign-in for inactive account: {}", email);
                throw new RuntimeException("Account is inactive. Please contact support.");
            }
        }

        // Load UserDetails for the identified/created user
        UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername(user.getUsername());
        logger.debug("UserDetails loaded for user: {}", user.getUsername());

        // Create an authenticated token directly
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        String refreshToken = refreshTokenService.createRefreshToken(userDetails.getId()).getToken();
        logger.info("Facebook Sign-In successful for user: {}", email);

        return generateJwtResponse(userDetails, refreshToken, user);
    }

    public JwtResponse refreshToken(String requestRefreshToken) {
        RefreshToken refreshToken = refreshTokenService.findByToken(requestRefreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));

        refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();
        UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername(user.getUsername());

        String newJwt = jwtService.generateJwtToken(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        return generateJwtResponse(userDetails, requestRefreshToken, user);
    }

    public void verifyOtp(String email, String otp) {
        otpService.verifyOtp(email, otp);
        // OTP is verified, no further action needed here for now.
        // The client can then call resetPassword.
    }

    public void forgotPassword(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new RuntimeException("Password reset is not allowed for inactive accounts.");
            }
            String otp = otpService.createOtp(email);
            emailService.sendOtpEmail(email, otp);
        }
        // Still return success to prevent user enumeration, even if email not found or status is not active
        // The AuthController will handle the RuntimeException for inactive accounts.
    }

    public void resetPassword(String email, String otp, String newPassword) {
        otpService.verifyOtp(email, otp);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Error: Email not found."));
        user.setPassword(encoder.encode(newPassword));
        userRepository.save(user);

        otpService.deleteOtp(email, otp);
    }

    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User("admin", "admin@maepim.com", encoder.encode("Admin@123"));
            Set<Role> roles = new HashSet<>();
            Role adminRole = roleRepository.findByRoleName("ROLE_SUPER_ADMIN")
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(adminRole);
            admin.setRoles(roles);
            admin.setStatus(UserStatus.ACTIVE);
            userRepository.save(admin);
        }
    }

    private JwtResponse generateJwtResponse(UserDetailsImpl userDetails, String refreshToken, User user) {
        String jwt = jwtService.generateJwtToken(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())); // Generate JWT from authenticated UserDetails

        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return new JwtResponse(jwt, refreshToken, userDetails.getId(),
                userDetails.getUsername(), userDetails.getEmail(),
                user.getFirstName(), user.getLastName(), user.getPhone(), user.getStatus(), roles);
    }
}