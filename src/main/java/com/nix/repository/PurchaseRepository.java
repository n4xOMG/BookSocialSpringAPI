package com.nix.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nix.models.Purchase;
import com.nix.models.User;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

	boolean existsByPaymentIntentId(String paymentIntentId);

	List<Purchase> findByUser(User user);
}
