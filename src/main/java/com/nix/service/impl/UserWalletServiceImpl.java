package com.nix.service.impl;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nix.exception.ResourceNotFoundException;
import com.nix.models.User;
import com.nix.models.UserWallet;
import com.nix.repository.UserRepository;
import com.nix.repository.UserWalletRepository;
import com.nix.service.UserWalletService;

@Service
@Transactional
public class UserWalletServiceImpl implements UserWalletService {

    private final UserWalletRepository userWalletRepository;
    private final UserRepository userRepository;

    public UserWalletServiceImpl(UserWalletRepository userWalletRepository, UserRepository userRepository) {
        this.userWalletRepository = userWalletRepository;
        this.userRepository = userRepository;
    }

    @Override
    public UserWallet getOrCreateWallet(UUID userId) {
        return userWalletRepository.findById(userId).orElseGet(() -> createWallet(userId));
    }

    @Override
    public UserWallet addCredits(UUID userId, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount to add must be positive");
        }
        UserWallet wallet = getOrCreateWallet(userId);
        wallet.setBalance(wallet.getBalance() + amount);
        wallet.setLastUpdated(LocalDateTime.now());
        return userWalletRepository.save(wallet);
    }

    @Override
    public UserWallet deductCredits(UUID userId, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount to deduct must be positive");
        }
        UserWallet wallet = getOrCreateWallet(userId);
        if (wallet.getBalance() < amount) {
            throw new IllegalStateException("Insufficient credits");
        }
        wallet.setBalance(wallet.getBalance() - amount);
        wallet.setLastUpdated(LocalDateTime.now());
        return userWalletRepository.save(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public int getBalance(UUID userId) {
        return userWalletRepository.findById(userId).map(UserWallet::getBalance).orElse(0);
    }

    @Override
    public void deleteWallet(UUID userId) {
        userWalletRepository.findById(userId).ifPresent(userWalletRepository::delete);
    }

    private UserWallet createWallet(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        UserWallet wallet = new UserWallet();
        wallet.setUser(user);
        wallet.setBalance(0);
        wallet.setLastUpdated(LocalDateTime.now());
    user.setWallet(wallet);
        return userWalletRepository.save(wallet);
    }
}
