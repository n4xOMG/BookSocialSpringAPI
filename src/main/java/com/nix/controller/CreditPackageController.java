package com.nix.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.nix.response.ApiResponseWithData;
import com.nix.service.CreditPackageService;

@RestController
public class CreditPackageController {
	@Autowired
	CreditPackageService creditPackageService;

	@Autowired
	CreditPackageMapper cpkMapper;

	@GetMapping("/admin/credit-packages")
	@PreAuthorize("hasAnyRole('ADMIN')")
	public ResponseEntity<ApiResponseWithData<List<CreditPackageDTO>>> getAllCreditPackages() {
		try {
			List<CreditPackage> packages = creditPackageService.getAllCreditPackages();
			return buildSuccessResponse("Credit packages retrieved successfully.", cpkMapper.mapToDTOs(packages));
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to retrieve credit packages: " + e.getMessage());
		}
	}

	// Get active credit packages
	@GetMapping("/credit-packages/active")
	public ResponseEntity<ApiResponseWithData<List<CreditPackageDTO>>> getActiveCreditPackages() {
		try {
			List<CreditPackage> activePackages = creditPackageService.getActiveCreditPackages();
			return buildSuccessResponse("Active credit packages retrieved successfully.",
					cpkMapper.mapToDTOs(activePackages));
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to retrieve active credit packages: " + e.getMessage());
		}
	}

	// Get credit package by ID
	@GetMapping("/credit-packages/{id}")
	public ResponseEntity<ApiResponseWithData<CreditPackageDTO>> getCreditPackageById(@PathVariable Long id) {
		try {
			CreditPackage cp = creditPackageService.getCreditPackageById(id);
			return buildSuccessResponse("Credit package retrieved successfully.", cpkMapper.mapToDTO(cp));
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.NOT_FOUND,
					"Credit package not found: " + e.getMessage());
		}
	}

	// Create a new credit package
	@PostMapping("/admin/credit-packages")
	@PreAuthorize("hasAnyRole('ADMIN')")
	public ResponseEntity<ApiResponseWithData<CreditPackageDTO>> createCreditPackage(
			@RequestBody CreditPackage creditPackage) {
		try {
			CreditPackage created = creditPackageService.createCreditPackage(creditPackage);
			return buildSuccessResponse(HttpStatus.CREATED, "Credit package created successfully.",
					cpkMapper.mapToDTO(created));
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.BAD_REQUEST,
					"Failed to create credit package: " + e.getMessage());
		}
	}

	// Update an existing credit package
	@PutMapping("/admin/credit-packages/{id}")
	@PreAuthorize("hasAnyRole('ADMIN')")
	public ResponseEntity<ApiResponseWithData<CreditPackageDTO>> updateCreditPackage(@PathVariable Long id,
			@RequestBody CreditPackage creditPackage) {
		try {
			CreditPackage updated = creditPackageService.updateCreditPackage(id, creditPackage);
			return buildSuccessResponse("Credit package updated successfully.", cpkMapper.mapToDTO(updated));
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.BAD_REQUEST,
					"Failed to update credit package: " + e.getMessage());
		}
	}

	// Delete a credit package
	@DeleteMapping("/admin/credit-packages/{id}")
	@PreAuthorize("hasAnyRole('ADMIN')")
	public ResponseEntity<ApiResponseWithData<Void>> deleteCreditPackage(@PathVariable Long id) {
		try {
			creditPackageService.deleteCreditPackage(id);
			return buildSuccessResponse("Credit package deleted successfully.", null);
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to delete credit package: " + e.getMessage());
		}
	}

	// Search credit packages by name
	@GetMapping("/credit-packages/search")
	public ResponseEntity<ApiResponseWithData<List<CreditPackageDTO>>> searchCreditPackagesByName(
			@RequestParam String name) {
		try {
			List<CreditPackage> results = creditPackageService.searchCreditPackagesByName(name);
			return buildSuccessResponse("Credit packages retrieved successfully.", cpkMapper.mapToDTOs(results));
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to search credit packages: " + e.getMessage());
		}
	}

	// Get credit packages by maximum price
	@GetMapping("/credit-packages/price")
	public ResponseEntity<ApiResponseWithData<List<CreditPackageDTO>>> getCreditPackagesByPrice(
			@RequestParam double maxPrice) {
		try {
			List<CreditPackage> results = creditPackageService.getCreditPackagesByPrice(maxPrice);
			return buildSuccessResponse("Credit packages retrieved successfully.", cpkMapper.mapToDTOs(results));
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to retrieve credit packages by price: " + e.getMessage());
		}
	}

	// Get credit packages sorted by credit amount in descending order
	@GetMapping("/credit-packages/sorted-by-credit")
	public ResponseEntity<ApiResponseWithData<List<CreditPackageDTO>>> getCreditPackagesSortedByCreditAmountDesc() {
		try {
			List<CreditPackage> sortedPackages = creditPackageService.getCreditPackagesSortedByCreditAmountDesc();
			return buildSuccessResponse("Credit packages retrieved successfully.", cpkMapper.mapToDTOs(sortedPackages));
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to retrieve sorted credit packages: " + e.getMessage());
		}
	}

	// Activate a credit package
	@PutMapping("/admin/credit-packages/{id}/activate")
	@PreAuthorize("hasAnyRole('ADMIN')")
	public ResponseEntity<ApiResponseWithData<CreditPackageDTO>> activateCreditPackage(@PathVariable Long id) {
		try {
			CreditPackage activated = creditPackageService.activateCreditPackage(id);
			return buildSuccessResponse("Credit package activated successfully.", cpkMapper.mapToDTO(activated));
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.BAD_REQUEST,
					"Failed to activate credit package: " + e.getMessage());
		}
	}

	// Deactivate a credit package
	@PutMapping("/admin/credit-packages/{id}/deactivate")
	@PreAuthorize("hasAnyRole('ADMIN')")
	public ResponseEntity<ApiResponseWithData<CreditPackageDTO>> deactivateCreditPackage(@PathVariable Long id) {
		try {
			CreditPackage deactivated = creditPackageService.deactivateCreditPackage(id);
			return buildSuccessResponse("Credit package deactivated successfully.",
					cpkMapper.mapToDTO(deactivated));
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.BAD_REQUEST,
					"Failed to deactivate credit package: " + e.getMessage());
		}
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildSuccessResponse(String message, T data) {
		return buildSuccessResponse(HttpStatus.OK, message, data);
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildSuccessResponse(HttpStatus status, String message, T data) {
		return ResponseEntity.status(status).body(new ApiResponseWithData<>(message, true, data));
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildErrorResponse(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(new ApiResponseWithData<>(message, false));
	}
}
