package com.nix.service;

import java.util.UUID;

import com.nix.models.UserWallet;

public interface UserWalletService {

    UserWallet getOrCreateWallet(UUID userId);

    UserWallet addCredits(UUID userId, int amount);

    UserWallet deductCredits(UUID userId, int amount);

    int getBalance(UUID userId);

    void deleteWallet(UUID userId);
}
