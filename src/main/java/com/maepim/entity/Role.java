package com.maepim.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "roles")
public class Role {

    @Setter
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Getter
    @Column(name = "role_name", nullable = false, unique = true, length = 50)
    private String roleName;

    @Setter
    @Getter
    @Column(length = 255)
    private String description;

    @Setter
    @Getter
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Role() {
    }

    public Role(String roleName) {
        this.roleName = roleName;
    }


}