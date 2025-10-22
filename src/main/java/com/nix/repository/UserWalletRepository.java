package com.nix.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nix.models.UserWallet;

public interface UserWalletRepository extends JpaRepository<UserWallet, UUID> {
}
