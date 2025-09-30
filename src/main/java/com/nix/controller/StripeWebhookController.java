package com.nix.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nix.service.AuthorServiceImpl;
import com.nix.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.Transfer;
import com.stripe.net.Webhook;

@RestController
@RequestMapping("/api/webhooks")
public class StripeWebhookController {

	@Value("${stripe.webhook.secret:}")
	private String webhookSecret;

	@Autowired
	private AuthorServiceImpl authorService; // Using Impl to access webhook handling

	@Autowired
	private PaymentService paymentService;

	@PostMapping("/stripe")
	public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload,
			@RequestHeader("Stripe-Signature") String sigHeader) {

		if (webhookSecret.isEmpty()) {
			// If no webhook secret is configured, skip verification
			return processWebhookPayload(payload);
		}
		System.out.println("Webhook payload: " + payload);
		System.out.println("Stripe-Signature: " + sigHeader);
		System.out.println("Expected webhook secret: " + webhookSecret);
		Event event;
		try {
			event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
		} catch (SignatureVerificationException e) {
			// Invalid signature
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
		}

		return processWebhookEvent(event);
	}

	private ResponseEntity<String> processWebhookPayload(String payload) {
		try {
			Event event = Event.GSON.fromJson(payload, Event.class);
			return processWebhookEvent(event);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");
		}
	}

	private ResponseEntity<String> processWebhookEvent(Event event) {
		try {
			// Handle payment events (customer payments)
			if ("payment_intent.succeeded".equals(event.getType())) {
				System.out.println("Processing payment_intent.succeeded event");
				StripeObject stripeObject = event.getDataObjectDeserializer().getObject().orElse(null);

				if (stripeObject instanceof PaymentIntent) {
					PaymentIntent paymentIntent = (PaymentIntent) stripeObject;
					System.out.println("PaymentIntent ID: " + paymentIntent.getId());
					System.out.println("PaymentIntent Status: " + paymentIntent.getStatus());
					System.out.println("PaymentIntent Metadata: " + paymentIntent.getMetadata());

					// Extract metadata
					String userIdStr = paymentIntent.getMetadata().get("userId");
					String creditPackageIdStr = paymentIntent.getMetadata().get("creditPackageId");

					if (userIdStr != null && creditPackageIdStr != null) {
						UUID userId = UUID.fromString(userIdStr);
						Long creditPackageId = Long.parseLong(creditPackageIdStr);

						// Process payment confirmation
						paymentService.confirmPayment(userId, creditPackageId, paymentIntent.getId());
						System.out.println("Payment confirmed for user: " + userId + ", package: " + creditPackageId);
					} else {
						System.err.println(
								"Missing metadata - userId: " + userIdStr + ", creditPackageId: " + creditPackageIdStr);
					}
				} else {
					System.err.println("Expected PaymentIntent but got: "
							+ (stripeObject != null ? stripeObject.getClass().getName() : "null"));
				}
			}
			// Handle transfer events (payouts to authors)
			else if ("transfer.paid".equals(event.getType()) || "transfer.failed".equals(event.getType())) {
				StripeObject stripeObject = event.getDataObjectDeserializer().getObject().orElse(null);

				if (stripeObject instanceof Transfer) {
					Transfer transfer = (Transfer) stripeObject;
					String status = "transfer.paid".equals(event.getType()) ? "paid" : "failed";
					String failureReason = "transfer.failed".equals(event.getType()) ? "Transfer failed" : null;

					authorService.handleStripePayoutWebhook(transfer.getId(), status, failureReason);
				}
			}

			return ResponseEntity.ok("Webhook processed successfully");

		} catch (Exception e) {
			// Log the error
			System.err.println("Error processing webhook: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing webhook");
		}
	}
}