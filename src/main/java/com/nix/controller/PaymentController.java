package com.nix.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.nix.enums.PaymentProvider;
import com.nix.models.CreditPackage;
import com.nix.models.User;
import com.nix.request.ConfirmPaymentRequest;
import com.nix.request.PurchaseRequest;
import com.nix.service.PaymentService;
import com.nix.service.UserService;
import com.paypal.sdk.models.Order;

@RestController
public class PaymentController {

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private UserService userService;

	/**
	 * Get available payment providers
	 */
	@GetMapping("/api/payments/providers")
	public ResponseEntity<Map<String, Object>> getPaymentProviders() {
		List<String> providers = Arrays.stream(PaymentProvider.values()).map(Enum::toString)
				.collect(Collectors.toList());

		return ResponseEntity.ok(Map.of("providers", providers, "default", PaymentProvider.STRIPE.toString()));
	}

	/**
	 * Legacy endpoint for Stripe-only payments (maintains backward compatibility)
	 */
	@PostMapping("/api/payments/create-payment-intent")
	public ResponseEntity<Map<String, String>> createPaymentIntent(@RequestBody PurchaseRequest paymentRequest,
			@RequestHeader("Authorization") String jwt) {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return ResponseEntity.status(401).body(Map.of("error", "User not found."));
			}

			CreditPackage creditPackage = paymentService.getCreditPackageById(paymentRequest.getCreditPackageId());
			if (creditPackage == null || !creditPackage.isActive()) {
				return ResponseEntity.badRequest().body(Map.of("error", "Invalid credit package."));
			}

			long amount = (long) (creditPackage.getPrice() * 100);
			String clientSecret = paymentService.createStripePaymentIntent(amount, paymentRequest.getCurrency(),
					user.getId(), creditPackage.getId());

			return ResponseEntity.ok(Map.of("clientSecret", clientSecret));

		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
		}
	}

	/**
	 * Unified endpoint for creating Stripe payments. PayPal payments are handled
	 * entirely client-side using react-paypal-js.
	 */
	@PostMapping("/api/payments/create-payment")
	public ResponseEntity<?> createPayment(@RequestBody PurchaseRequest paymentRequest,
			@RequestHeader("Authorization") String jwt) {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return ResponseEntity.status(401).body(Map.of("error", "User not found."));
			}

			CreditPackage creditPackage = paymentService.getCreditPackageById(paymentRequest.getCreditPackageId());
			if (creditPackage == null || !creditPackage.isActive()) {
				return ResponseEntity.badRequest().body(Map.of("error", "Invalid credit package."));
			}

			// Default to Stripe if no provider specified
			PaymentProvider provider = paymentRequest.getPaymentProvider() != null ? paymentRequest.getPaymentProvider()
					: PaymentProvider.STRIPE;

			if (provider == PaymentProvider.STRIPE) {
				long amount = (long) (creditPackage.getPrice() * 100);
				String clientSecret = paymentService.createStripePaymentIntent(amount, paymentRequest.getCurrency(),
						user.getId(), creditPackage.getId());

				return ResponseEntity.ok(Map.of("clientSecret", clientSecret, "provider", "STRIPE", "packageId",
						creditPackage.getId(), "amount", creditPackage.getPrice()));
			} else if (provider == PaymentProvider.PAYPAL) {
				Order order = paymentService.createPaypalOrder(user.getId(), creditPackage.getId());
				return new ResponseEntity<>(order, HttpStatus.OK);
			} else {
				return ResponseEntity.badRequest().body(Map.of("error", "Unsupported payment provider: " + provider));
			}

		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
		}
	}

	/**
	 * Confirm payment and update user credits for both Stripe and PayPal
	 */
	@PostMapping("/api/payments/confirm-payment")
	public ResponseEntity<Map<String, String>> confirmPayment(@RequestBody ConfirmPaymentRequest confirmPaymentRequest,
			@RequestHeader("Authorization") String jwt) {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return ResponseEntity.status(401).body(Map.of("error", "User not found."));
			}

			PaymentProvider provider = confirmPaymentRequest.getPaymentProvider() != null
					? confirmPaymentRequest.getPaymentProvider()
					: PaymentProvider.STRIPE;

			paymentService.confirmPayment(user.getId(), confirmPaymentRequest.getCreditPackageId(),
					confirmPaymentRequest.getPaymentIntentId(), provider);

			return ResponseEntity
					.ok(Map.of("message", "Payment confirmed and credits updated.", "provider", provider.toString()));

		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
		}
	}

	/**
	 * Capture PayPal order after user approval
	 */
	@PostMapping("/api/orders/{orderID}/capture")
	public ResponseEntity<Order> capturePaypalOrder(@PathVariable String orderID) {
		try {
			Order response = paymentService.capturePaypalOrders(orderID);
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}
}
