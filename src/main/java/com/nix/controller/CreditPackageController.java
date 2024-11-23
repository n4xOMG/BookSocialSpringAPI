package com.nix.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nix.models.CreditPackage;
import com.nix.service.CreditPackageService;

@RestController
public class CreditPackageController {
	@Autowired
	CreditPackageService creditPackageService;

	@GetMapping("/credit-packages")
	public ResponseEntity<List<CreditPackage>> getAllCreditPackages() {
		List<CreditPackage> packages = creditPackageService.getAllCreditPackages();
		return ResponseEntity.ok(packages);
	}

	// Get active credit packages
	@GetMapping("/credit-packages/active")
	public ResponseEntity<List<CreditPackage>> getActiveCreditPackages() {
		List<CreditPackage> activePackages = creditPackageService.getActiveCreditPackages();
		return ResponseEntity.ok(activePackages);
	}

	// Get credit package by ID
	@GetMapping("/credit-packages/{id}")
	public ResponseEntity<CreditPackage> getCreditPackageById(@PathVariable Integer id) {
		CreditPackage cp = creditPackageService.getCreditPackageById(id);
		return ResponseEntity.ok(cp);
	}

	// Create a new credit package
	@PostMapping("/api/credit-packages")
	public ResponseEntity<CreditPackage> createCreditPackage(@RequestBody CreditPackage creditPackage) {
		CreditPackage created = creditPackageService.createCreditPackage(creditPackage);
		return ResponseEntity.ok(created);
	}

	// Update an existing credit package
	@PutMapping("/api/credit-packages/{id}")
	public ResponseEntity<CreditPackage> updateCreditPackage(@PathVariable Integer id,
			@RequestBody CreditPackage creditPackage) {
		CreditPackage updated = creditPackageService.updateCreditPackage(id, creditPackage);
		return ResponseEntity.ok(updated);
	}

	// Delete a credit package
	@DeleteMapping("/api/credit-packages/{id}")
	public ResponseEntity<Void> deleteCreditPackage(@PathVariable Integer id) {
		creditPackageService.deleteCreditPackage(id);
		return ResponseEntity.noContent().build();
	}

	// Search credit packages by name
	@GetMapping("/credit-packages/search")
	public ResponseEntity<List<CreditPackage>> searchCreditPackagesByName(@RequestParam String name) {
		List<CreditPackage> results = creditPackageService.searchCreditPackagesByName(name);
		return ResponseEntity.ok(results);
	}

	// Get credit packages by maximum price
	@GetMapping("/credit-packages/price")
	public ResponseEntity<List<CreditPackage>> getCreditPackagesByPrice(@RequestParam double maxPrice) {
		List<CreditPackage> results = creditPackageService.getCreditPackagesByPrice(maxPrice);
		return ResponseEntity.ok(results);
	}

	// Get credit packages sorted by credit amount in descending order
	@GetMapping("/credit-packages/sorted-by-credit")
	public ResponseEntity<List<CreditPackage>> getCreditPackagesSortedByCreditAmountDesc() {
		List<CreditPackage> sortedPackages = creditPackageService.getCreditPackagesSortedByCreditAmountDesc();
		return ResponseEntity.ok(sortedPackages);
	}

	// Activate a credit package
	@PutMapping("/api/credit-packages/{id}/activate")
	public ResponseEntity<CreditPackage> activateCreditPackage(@PathVariable Integer id) {
		CreditPackage activated = creditPackageService.activateCreditPackage(id);
		return ResponseEntity.ok(activated);
	}

	// Deactivate a credit package
	@PutMapping("/api/credit-packages/{id}/deactivate")
	public ResponseEntity<CreditPackage> deactivateCreditPackage(@PathVariable Integer id) {
		CreditPackage deactivated = creditPackageService.deactivateCreditPackage(id);
		return ResponseEntity.ok(deactivated);
	}
}
