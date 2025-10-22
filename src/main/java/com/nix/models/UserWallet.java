package com.nix.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserWallet implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private UUID id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    private User user;

    @Column(nullable = false)
    private int balance;

    private LocalDateTime lastUpdated;
}
