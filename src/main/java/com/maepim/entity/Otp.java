package com.maepim.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "otp_codes")
public class Otp {

    @Setter
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Getter
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Setter
    @Getter
    @Column(nullable = false)
    private String code;

    @Setter
    @Getter
    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    public Otp() {
    }
}