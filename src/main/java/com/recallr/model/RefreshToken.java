package com.recallr.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
public class RefreshToken {
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String token;        // UUID
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    private Instant expiresAt;
    private boolean revoked;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}