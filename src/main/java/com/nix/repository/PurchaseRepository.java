package com.nix.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nix.models.Purchase;

public interface PurchaseRepository extends JpaRepository<Purchase, Integer> {

	boolean existsByPaymentIntentId(String paymentIntentId);

}
