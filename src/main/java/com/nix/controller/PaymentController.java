package com.nix.controller;

import java.security.Principal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nix.models.CreditPackage;
import com.nix.models.User;
import com.nix.request.ConfirmPaymentRequest;
import com.nix.request.PurchaseRequest;
import com.nix.service.PaymentService;
import com.nix.service.UserService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

@RestController
public class PaymentController {

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private UserService userService;

	@PostMapping("/api/payments/create-payment-intent")
	public ResponseEntity<Map<String, String>> createPaymentIntent(@RequestBody PurchaseRequest paymentRequest,
			@RequestHeader("Authorization") String jwt) {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return ResponseEntity.status(401).body(Map.of("error", "User not found."));
			}

			// Validate credit package
			CreditPackage creditPackage = paymentService.getCreditPackageById(paymentRequest.getCreditPackageId());
			if (creditPackage == null || !creditPackage.isActive()) {
				return ResponseEntity.badRequest().body(Map.of("error", "Invalid credit package."));
			}

			// Calculate amount in cents
			long amount = (long) (creditPackage.getPrice() * 100); // e.g., $10.00 -> 1000 cents

			// Create Payment Intent
			String clientSecret = paymentService.createPaymentIntent(amount, paymentRequest.getCurrency(), user.getId(),
					creditPackage.getId());

			return ResponseEntity.ok(Map.of("clientSecret", clientSecret));
		} catch (StripeException e) {
			return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
		}
	}

	/**
	 * Confirm payment and update user credits
	 */
	@PostMapping("/api/payments/confirm-payment")
	public ResponseEntity<Map<String, String>> confirmPayment(@RequestBody ConfirmPaymentRequest confirmPaymentRequest,
			@RequestHeader("Authorization") String jwt) {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return ResponseEntity.status(401).body(Map.of("error", "User not found."));
			}

			// Retrieve Payment Intent from Stripe to verify status
			PaymentIntent paymentIntent = PaymentIntent.retrieve(confirmPaymentRequest.getPaymentIntentId());

			if ("succeeded".equals(paymentIntent.getStatus())) {
				// Confirm payment and update credits
				paymentService.confirmPayment(user.getId(), confirmPaymentRequest.getCreditPackageId(),
						paymentIntent.getId());
				return ResponseEntity.ok(Map.of("message", "Payment confirmed and credits updated."));
			} else {
				return ResponseEntity.badRequest().body(Map.of("error", "Payment not successful."));
			}
		} catch (StripeException e) {
			return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
		}
	}
}
