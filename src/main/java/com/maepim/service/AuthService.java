package com.maepim.service;

import com.maepim.dto.request.AddressRequest;
import com.maepim.dto.request.LoginRequest;
import com.maepim.dto.request.SignupRequest;
import com.maepim.dto.response.JwtResponse;
import com.maepim.entity.Address;
import com.maepim.entity.Role;
import com.maepim.entity.UserStatus;
import com.maepim.entity.User;
import com.maepim.repository.AddressRepository;
import com.maepim.repository.RoleRepository;
import com.maepim.repository.UserRepository;
import com.maepim.security.jwt.JwtService;
import com.maepim.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService implements CommandLineRunner {

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

    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtService.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        String refreshToken = refreshTokenService.createRefreshToken(userDetails.getId()).getToken();

        return new JwtResponse(jwt, refreshToken, userDetails.getId(),
                userDetails.getUsername(), userDetails.getEmail(), roles);
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

    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User("admin", "admin@maepim.com", encoder.encode("Admin@123"));
            Set<Role> roles = new HashSet<>();
            Role adminRole = roleRepository.findByRoleName("ROLE_SUPER_ADMIN")
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(adminRole);
            admin.setRoles(roles);
            // Set the status for the initial admin user
            admin.setStatus(UserStatus.ACTIVE);
            userRepository.save(admin);
        }
    }
}