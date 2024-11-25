package com.nix.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nix.models.CreditPackage;

public interface CreditPackageRepository extends JpaRepository<CreditPackage, Long> {
	List<CreditPackage> findByIsActiveTrue();

	List<CreditPackage> findByIsActiveFalse();

	List<CreditPackage> findByNameContainingIgnoreCase(String name);

	boolean existsByName(String name);

	List<CreditPackage> findByPriceLessThanEqual(double price);

	List<CreditPackage> findAllByOrderByCreditAmountDesc();
}
