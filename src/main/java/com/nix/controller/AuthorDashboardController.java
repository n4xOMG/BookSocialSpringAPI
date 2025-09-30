package com.nix.controller;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nix.dtos.AuthorDashboardDTO;
import com.nix.dtos.AuthorEarningDTO;
import com.nix.dtos.AuthorPayoutDTO;
import com.nix.dtos.AuthorPayoutSettingsDTO;
import com.nix.models.AuthorPayoutSettings;
import com.nix.models.User;
import com.nix.request.PayoutRequestDTO;
import com.nix.service.AuthorService;
import com.nix.service.UserService;

@RestController
@RequestMapping("/api/author")
public class AuthorDashboardController {

	@Autowired
	private UserService userService;

	@Autowired
	private AuthorService authorService;

	/**
	 * Get author dashboard overview
	 */
	@GetMapping("/dashboard")
	public ResponseEntity<?> getAuthorDashboard(@RequestHeader("Authorization") String jwt,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "id") String sortBy) {
		try {
			User author = userService.findUserByJwt(jwt);
			if (author == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
			}

			Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
			AuthorDashboardDTO dashboard = authorService.getAuthorDashboard(author, pageable);
			return ResponseEntity.ok(dashboard);

		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error fetching dashboard: " + e.getMessage());
		}
	}

	/**
	 * Get author earnings with pagination
	 */
	@GetMapping("/earnings")
	public ResponseEntity<?> getAuthorEarnings(@RequestHeader("Authorization") String jwt,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		try {
			User author = userService.findUserByJwt(jwt);
			if (author == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
			}

			Pageable pageable = PageRequest.of(page, size);
			Page<AuthorEarningDTO> earnings = authorService.getAuthorEarnings(author, pageable);
			return ResponseEntity.ok(earnings);

		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error fetching earnings: " + e.getMessage());
		}
	}

	/**
	 * Get author payout history
	 */
	@GetMapping("/payouts")
	public ResponseEntity<?> getAuthorPayouts(@RequestHeader("Authorization") String jwt,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		try {
			User author = userService.findUserByJwt(jwt);
			if (author == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
			}

			Pageable pageable = PageRequest.of(page, size);
			Page<AuthorPayoutDTO> payouts = authorService.getAuthorPayouts(author, pageable);
			return ResponseEntity.ok(payouts);

		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error fetching payouts: " + e.getMessage());
		}
	}

	/**
	 * Request a payout
	 */
	@PostMapping("/payouts/request")
	public ResponseEntity<?> requestPayout(@RequestHeader("Authorization") String jwt,
			@RequestBody PayoutRequestDTO payoutRequest) {
		try {
			User author = userService.findUserByJwt(jwt);
			if (author == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
			}

			// Validate payout amount
			BigDecimal availableBalance = authorService.getUnpaidEarnings(author);
			if (payoutRequest.getAmount().compareTo(availableBalance) > 0) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Requested amount exceeds available balance");
			}

			if (!authorService.canRequestPayout(author)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body("Payout requirements not met. Check minimum amount and Stripe connection.");
			}

			var payout = authorService.requestPayout(author, payoutRequest.getAmount());
			return ResponseEntity.ok("Payout requested successfully. Payout ID: " + payout.getId());

		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error requesting payout: " + e.getMessage());
		}
	}

	/**
	 * Update payout settings
	 */
	@PutMapping("/payout-settings")
	public ResponseEntity<?> updatePayoutSettings(@RequestHeader("Authorization") String jwt,
			@RequestBody AuthorPayoutSettingsDTO dto) {
		try {
			User author = userService.findUserByJwt(jwt);
			if (author == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
			}

			AuthorPayoutSettings toUpdate = new AuthorPayoutSettings();
			toUpdate.setMinimumPayoutAmount(dto.getMinimumPayoutAmount());
			toUpdate.setPayoutFrequency(dto.getPayoutFrequency());
			toUpdate.setAutoPayoutEnabled(dto.isAutoPayoutEnabled());
			toUpdate.setPaypalEmail(dto.getPaypalEmail());
			toUpdate.setPaymentMethodType(dto.getPaymentMethodType());
			toUpdate.setAccountHolderName(dto.getAccountHolderName());
			toUpdate.setBankName(dto.getBankName());
			toUpdate.setAccountLastFour(dto.getAccountLastFour());

			AuthorPayoutSettingsDTO updated = authorService.updatePayoutSettings(author, toUpdate);
			return ResponseEntity.ok(updated);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error updating payout settings: " + e.getMessage());
		}
	}

	/**
	 * Get payout settings
	 */
	@GetMapping("/payout-settings")
	public ResponseEntity<?> getPayoutSettings(@RequestHeader("Authorization") String jwt) {
		try {
			User author = userService.findUserByJwt(jwt);
			if (author == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
			}

			AuthorPayoutSettingsDTO settings = authorService.getPayoutSettings(author);
			return ResponseEntity.ok(settings);

		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error fetching payout settings: " + e.getMessage());
		}
	}
}