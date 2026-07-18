package com.maepim.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

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
    @Column(nullable = false, unique = true)
    private String token;

    @Setter
    @Getter
    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    @Setter
    @Getter
    @Column
    private boolean revoked = false;

    @Setter
    @Getter
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public RefreshToken() {
    }


}