package com.nix.controller;

import java.util.Arrays;
import java.util.HashMap;
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
import com.nix.response.ApiResponseWithData;
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
	public ResponseEntity<ApiResponseWithData<Map<String, Object>>> getPaymentProviders() {
		List<String> providers = Arrays.stream(PaymentProvider.values()).map(Enum::toString)
				.collect(Collectors.toList());

		Map<String, Object> data = new HashMap<>();
		data.put("providers", providers);
		data.put("default", PaymentProvider.STRIPE.toString());

		return buildSuccessResponse("Payment providers retrieved successfully.", data);
	}

	/**
	 * Legacy endpoint for Stripe-only payments
	 */
	@PostMapping("/api/payments/create-payment-intent")
	public ResponseEntity<ApiResponseWithData<Map<String, String>>> createPaymentIntent(
			@RequestBody PurchaseRequest paymentRequest,
			@RequestHeader("Authorization") String jwt) {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return this.<Map<String, String>>buildErrorResponse(HttpStatus.UNAUTHORIZED, "User not found.");
			}

			CreditPackage creditPackage = paymentService.getCreditPackageById(paymentRequest.getCreditPackageId());
			if (creditPackage == null || !creditPackage.isActive()) {
				return this.<Map<String, String>>buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid credit package.");
			}

			long amount = (long) (creditPackage.getPrice() * 100);
			String clientSecret = paymentService.createStripePaymentIntent(amount, paymentRequest.getCurrency(),
					user.getId(), creditPackage.getId());

			Map<String, String> data = new HashMap<>();
			data.put("clientSecret", clientSecret);

			return buildSuccessResponse("Payment intent created successfully.", data);

		} catch (Exception e) {
			return this.<Map<String, String>>buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	/**
	 * Unified endpoint for creating Stripe payments.
	 */
	@PostMapping("/api/payments/create-payment")
	public ResponseEntity<ApiResponseWithData<Object>> createPayment(@RequestBody PurchaseRequest paymentRequest,
			@RequestHeader("Authorization") String jwt) {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return this.<Object>buildErrorResponse(HttpStatus.UNAUTHORIZED, "User not found.");
			}

			CreditPackage creditPackage = paymentService.getCreditPackageById(paymentRequest.getCreditPackageId());
			if (creditPackage == null || !creditPackage.isActive()) {
				return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid credit package.");
			}

			// Default to Stripe if no provider specified
			PaymentProvider provider = paymentRequest.getPaymentProvider() != null ? paymentRequest.getPaymentProvider()
					: PaymentProvider.STRIPE;

			if (provider == PaymentProvider.STRIPE) {
				long amount = (long) (creditPackage.getPrice() * 100);
				String clientSecret = paymentService.createStripePaymentIntent(amount, paymentRequest.getCurrency(),
						user.getId(), creditPackage.getId());

				Map<String, Object> data = new HashMap<>();
				data.put("clientSecret", clientSecret);
				data.put("provider", "STRIPE");
				data.put("packageId", creditPackage.getId());
				data.put("amount", creditPackage.getPrice());

				return ResponseEntity
						.ok(new ApiResponseWithData<>("Payment created successfully.", true, (Object) data));
			} else if (provider == PaymentProvider.PAYPAL) {
				Order order = paymentService.createPaypalOrder(user.getId(), creditPackage.getId());
				return ResponseEntity
						.ok(new ApiResponseWithData<>("PayPal order created successfully.", true, (Object) order));
			} else {
				return this.<Object>buildErrorResponse(HttpStatus.BAD_REQUEST,
						"Unsupported payment provider: " + provider);
			}

		} catch (Exception e) {
			return this.<Object>buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	/**
	 * Confirm payment and update user credits for both Stripe and PayPal
	 */
	@PostMapping("/api/payments/confirm-payment")
	public ResponseEntity<ApiResponseWithData<Map<String, String>>> confirmPayment(
			@RequestBody ConfirmPaymentRequest confirmPaymentRequest,
			@RequestHeader("Authorization") String jwt) {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return this.<Map<String, String>>buildErrorResponse(HttpStatus.UNAUTHORIZED, "User not found.");
			}

			PaymentProvider provider = confirmPaymentRequest.getPaymentProvider() != null
					? confirmPaymentRequest.getPaymentProvider()
					: PaymentProvider.STRIPE;

			paymentService.confirmPayment(user.getId(), confirmPaymentRequest.getCreditPackageId(),
					confirmPaymentRequest.getPaymentIntentId(), provider);

			Map<String, String> data = new HashMap<>();
			data.put("message", "Payment confirmed and credits updated.");
			data.put("provider", provider.toString());

			return buildSuccessResponse("Payment confirmed successfully.", data);

		} catch (Exception e) {
			return this.<Map<String, String>>buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	/**
	 * Capture PayPal order after user approval
	 */
	@PostMapping("/api/orders/{orderID}/capture")
	public ResponseEntity<ApiResponseWithData<Order>> capturePaypalOrder(
			@PathVariable String orderID,
			@RequestHeader("Authorization") String jwt) {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return this.<Order>buildErrorResponse(HttpStatus.UNAUTHORIZED, "User not authenticated.");
			}

			// First, get the order details BEFORE capture to verify ownership
			Order orderDetails = paymentService.getPaypalOrder(orderID);

			// Verify that the order belongs to the authenticated user
			if (orderDetails.getPurchaseUnits() != null && !orderDetails.getPurchaseUnits().isEmpty()) {
				String orderUserId = orderDetails.getPurchaseUnits().get(0).getCustomId();
				if (orderUserId == null || !orderUserId.equals(user.getId().toString())) {
					return this.<Order>buildErrorResponse(HttpStatus.FORBIDDEN,
							"You are not authorized to capture this order.");
				}
			} else {
				return this.<Order>buildErrorResponse(HttpStatus.BAD_REQUEST,
						"Invalid order: no purchase units found.");
			}

			// Now capture the order (user is verified)
			Order capturedOrder = paymentService.capturePaypalOrders(orderID);

			return buildSuccessResponse("PayPal order captured successfully.", capturedOrder);
		} catch (Exception e) {
			return this.<Order>buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildSuccessResponse(String message, T data) {
		return ResponseEntity.ok(new ApiResponseWithData<>(message, true, data));
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildErrorResponse(HttpStatus status, String message) {
		ApiResponseWithData<T> response = new ApiResponseWithData<>(message, false, (T) null);
		return ResponseEntity.status(status).body(response);
	}
}
