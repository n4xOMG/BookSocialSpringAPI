package com.nix.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.nix.dtos.PurchaseDTO;
import com.nix.dtos.SalesPerUserDTO;
import com.nix.models.User;
import com.nix.response.ApiResponseWithData;
import com.nix.service.PurchaseService;
import com.nix.service.UserService;

@RestController
public class PurchaseController {

	@Autowired
	private PurchaseService purchaseService;

	@Autowired
	private UserService userService;

	@GetMapping("/api/purchases/history")
	public ResponseEntity<ApiResponseWithData<List<PurchaseDTO>>> getPurchaseHistory(
			@RequestHeader("Authorization") String jwt) {
		User user = userService.findUserByJwt(jwt);
		if (user == null) {
			return buildErrorResponse(HttpStatus.UNAUTHORIZED, "User not found.");
		}
		List<PurchaseDTO> purchaseHistory = purchaseService.getPurchaseHistoryForUser(user.getId());
		return buildSuccessResponse("Purchase history retrieved successfully.", purchaseHistory);
	}

	@GetMapping("/admin/purchases/history/users/{userId}")
	@PreAuthorize("hasAnyRole('ADMIN')")
	public ResponseEntity<ApiResponseWithData<List<PurchaseDTO>>> getPurchaseHistoryByUser(@PathVariable UUID userId) {
		List<PurchaseDTO> purchaseHistory = purchaseService.getPurchaseHistoryForUser(userId);
		return buildSuccessResponse("Purchase history retrieved successfully.", purchaseHistory);
	}

	@GetMapping("/admin/purchases/total-sales")
	@PreAuthorize("hasAnyRole('ADMIN')")
	public ResponseEntity<ApiResponseWithData<Double>> getTotalSalesAmount() {
		Double totalSales = purchaseService.getTotalSalesAmount();
		return buildSuccessResponse("Total sales retrieved successfully.", totalSales);
	}

	@GetMapping("/admin/purchases/purchases-count")
	@PreAuthorize("hasAnyRole('ADMIN')")
	public ResponseEntity<ApiResponseWithData<Long>> getTotalNumberOfPurchases() {
		Long totalPurchases = purchaseService.getTotalNumberOfPurchases();
		return buildSuccessResponse("Total purchases retrieved successfully.", totalPurchases);
	}

	@GetMapping("/admin/sales-per-user")
	@PreAuthorize("hasAnyRole('ADMIN')")
	public ResponseEntity<ApiResponseWithData<List<SalesPerUserDTO>>> getSalesStatisticsPerUser() {
		List<SalesPerUserDTO> salesPerUser = purchaseService.getSalesStatisticsPerUser();
		return buildSuccessResponse("Sales statistics retrieved successfully.", salesPerUser);
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildSuccessResponse(String message, T data) {
		return ResponseEntity.ok(new ApiResponseWithData<>(message, true, data));
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildErrorResponse(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(new ApiResponseWithData<>(message, false));
	}
}
