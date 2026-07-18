package com.maepim.service;

import com.maepim.entity.Otp;
import com.maepim.entity.User;
import com.maepim.repository.OtpRepository;
import com.maepim.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Random;

@Service
public class OtpService {

    @Value("${maepim.app.otpExpirationMs:300000}") // 5 minutes by default
    private Long otpExpirationMs;

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private UserRepository userRepository;

    public String createOtp(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Error: Email not found."));

        String code = generateOtp();

        Otp otp = new Otp();
        otp.setUser(user);
        otp.setCode(code);
        otp.setExpiryDate(Instant.now().plusMillis(otpExpirationMs));

        otpRepository.save(otp);

        return code;
    }

    public void verifyOtp(String userEmail, String code) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Error: Email not found."));

        Otp otp = otpRepository.findByUserIdAndCode(user.getId(), code)
                .orElseThrow(() -> new RuntimeException("Invalid OTP."));

        if (otp.getExpiryDate().isBefore(Instant.now())) {
            otpRepository.delete(otp);
            throw new RuntimeException("OTP has expired. Please request a new one.");
        }
    }

    @Transactional
    public void deleteOtp(String userEmail, String code) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Error: Email not found."));

        otpRepository.findByUserIdAndCode(user.getId(), code).ifPresent(otpRepository::delete);
    }

    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}