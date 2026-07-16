package com.maepim.entity;

import com.maepim.dto.request.AddressRequest;
import com.maepim.dto.request.SignupRequest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "addresses")
public class Address {

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
    @Column(name = "address_name", length = 100)
    private String addressName;

    @Setter
    @Getter
    @Column(name = "receiver_name", length = 255)
    private String receiverName;


    @Setter
    @Getter
    @Column(length = 20)
    private String phone;

    @Setter
    @Getter
    @Column(nullable = false)
    private String address;

    @Setter
    @Getter
    @Column(length = 100)
    private String subdistrict;

    @Setter
    @Getter
    @Column(length = 100)
    private String district;

    @Setter
    @Getter
    @Column(length = 100)
    private String province;

    @Setter
    @Getter
    @Column(name = "postal_code", length = 10)
    private String postalCode;

    @Column(name = "is_default")
    private boolean isDefault = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and Setters
}