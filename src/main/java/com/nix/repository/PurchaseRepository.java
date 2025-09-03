package com.nix.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nix.models.Purchase;
import com.nix.models.User;

public interface PurchaseRepository extends JpaRepository<Purchase, UUID> {

	boolean existsByPaymentIntentId(String paymentIntentId);

	List<Purchase> findByUser(User user);
}
