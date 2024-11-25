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

import com.nix.dtos.CreditPackageDTO;
import com.nix.dtos.mappers.CreditPackageMapper;
import com.nix.models.CreditPackage;
import com.nix.service.CreditPackageService;

@RestController
public class CreditPackageController {
	@Autowired
	CreditPackageService creditPackageService;

	CreditPackageMapper cpkMapper = new CreditPackageMapper();

	@GetMapping("/credit-packages")
	public ResponseEntity<List<CreditPackageDTO>> getAllCreditPackages() {
		List<CreditPackage> packages = creditPackageService.getAllCreditPackages();
		return ResponseEntity.ok(cpkMapper.mapToDTOs(packages));
	}

	// Get active credit packages
	@GetMapping("/credit-packages/active")
	public ResponseEntity<List<CreditPackageDTO>> getActiveCreditPackages() {
		List<CreditPackage> activePackages = creditPackageService.getActiveCreditPackages();
		return ResponseEntity.ok(cpkMapper.mapToDTOs(activePackages));
	}

	// Get credit package by ID
	@GetMapping("/credit-packages/{id}")
	public ResponseEntity<CreditPackageDTO> getCreditPackageById(@PathVariable Long id) {
		CreditPackage cp = creditPackageService.getCreditPackageById(id);
		return ResponseEntity.ok(cpkMapper.mapToDTO(cp));
	}

	// Create a new credit package
	@PostMapping("/api/credit-packages")
	public ResponseEntity<CreditPackageDTO> createCreditPackage(@RequestBody CreditPackage creditPackage) {
		CreditPackage created = creditPackageService.createCreditPackage(creditPackage);
		return ResponseEntity.ok(cpkMapper.mapToDTO(created));
	}

	// Update an existing credit package
	@PutMapping("/api/credit-packages/{id}")
	public ResponseEntity<CreditPackageDTO> updateCreditPackage(@PathVariable Long id,
			@RequestBody CreditPackage creditPackage) {
		CreditPackage updated = creditPackageService.updateCreditPackage(id, creditPackage);
		return ResponseEntity.ok(cpkMapper.mapToDTO(updated));
	}

	// Delete a credit package
	@DeleteMapping("/api/credit-packages/{id}")
	public ResponseEntity<Void> deleteCreditPackage(@PathVariable Long id) {
		creditPackageService.deleteCreditPackage(id);
		return ResponseEntity.noContent().build();
	}

	// Search credit packages by name
	@GetMapping("/credit-packages/search")
	public ResponseEntity<List<CreditPackageDTO>> searchCreditPackagesByName(@RequestParam String name) {
		List<CreditPackage> results = creditPackageService.searchCreditPackagesByName(name);
		return ResponseEntity.ok(cpkMapper.mapToDTOs(results));
	}

	// Get credit packages by maximum price
	@GetMapping("/credit-packages/price")
	public ResponseEntity<List<CreditPackageDTO>> getCreditPackagesByPrice(@RequestParam double maxPrice) {
		List<CreditPackage> results = creditPackageService.getCreditPackagesByPrice(maxPrice);
		return ResponseEntity.ok(cpkMapper.mapToDTOs(results));
	}

	// Get credit packages sorted by credit amount in descending order
	@GetMapping("/credit-packages/sorted-by-credit")
	public ResponseEntity<List<CreditPackageDTO>> getCreditPackagesSortedByCreditAmountDesc() {
		List<CreditPackage> sortedPackages = creditPackageService.getCreditPackagesSortedByCreditAmountDesc();
		return ResponseEntity.ok(cpkMapper.mapToDTOs(sortedPackages));
	}

	// Activate a credit package
	@PutMapping("/api/credit-packages/{id}/activate")
	public ResponseEntity<CreditPackageDTO> activateCreditPackage(@PathVariable Long id) {
		CreditPackage activated = creditPackageService.activateCreditPackage(id);
		return ResponseEntity.ok(cpkMapper.mapToDTO(activated));
	}

	// Deactivate a credit package
	@PutMapping("/api/credit-packages/{id}/deactivate")
	public ResponseEntity<CreditPackageDTO> deactivateCreditPackage(@PathVariable Long id) {
		CreditPackage deactivated = creditPackageService.deactivateCreditPackage(id);
		return ResponseEntity.ok(cpkMapper.mapToDTO(deactivated));
	}
}
