package com.nix.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nix.enums.NotificationEntityType;
import com.nix.enums.PaymentProvider;
import com.nix.enums.PaymentStatus;
import com.nix.models.CreditPackage;
import com.nix.models.Purchase;
import com.nix.models.User;
import com.nix.repository.CreditPackageRepository;
import com.nix.repository.PurchaseRepository;
import com.nix.repository.UserRepository;
import com.nix.service.NotificationService;
import com.nix.service.PaymentService;
import com.nix.service.UserWalletService;
import com.paypal.sdk.PaypalServerSdkClient;
import com.paypal.sdk.controllers.OrdersController;
import com.paypal.sdk.exceptions.ApiException;
import com.paypal.sdk.http.response.ApiResponse;
import com.paypal.sdk.models.AmountBreakdown;
import com.paypal.sdk.models.AmountWithBreakdown;
import com.paypal.sdk.models.CaptureOrderInput;
import com.paypal.sdk.models.CheckoutPaymentIntent;
import com.paypal.sdk.models.CreateOrderInput;
import com.paypal.sdk.models.Item;
import com.paypal.sdk.models.ItemCategory;
import com.paypal.sdk.models.Money;
import com.paypal.sdk.models.Order;
import com.paypal.sdk.models.OrderRequest;
import com.paypal.sdk.models.PurchaseUnitRequest;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;

@Service
public class PaymentServiceImpl implements PaymentService {
	@Value("${stripe.apiKey:}")
	private String stripeApiKey;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CreditPackageRepository creditPackageRepository;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private PurchaseRepository purchaseRepository;

	@Autowired
	private PaypalServerSdkClient client;

	@Autowired
	private UserWalletService userWalletService;

	@PostConstruct
	public void init() {
		if (stripeApiKey != null && !stripeApiKey.isBlank()) {
			com.stripe.Stripe.apiKey = stripeApiKey;
		}
	}

	@Override
	public String createStripePaymentIntent(long amount, String currency, UUID userId, Long creditPackageId)
			throws Exception {
		if (stripeApiKey == null || stripeApiKey.isBlank()) {
			throw new IllegalStateException("Stripe API key not configured");
		}

		PaymentIntentCreateParams params = PaymentIntentCreateParams.builder().setAmount(amount).setCurrency(currency)
				.putMetadata("userId", userId.toString()).putMetadata("creditPackageId", creditPackageId.toString())
				.build();

		PaymentIntent paymentIntent = PaymentIntent.create(params);
		return paymentIntent.getClientSecret();
	}

	@Override
	public List<CreditPackage> getAllActiveCreditPackages() {
		return creditPackageRepository.findByIsActiveTrue();
	}

	@Override
	public CreditPackage getCreditPackageById(Long id) {
		return creditPackageRepository.findById(id).orElse(null);
	}

	@Override
	@Transactional
	public void confirmPayment(UUID userId, Long creditPackageId, String paymentIntentId, PaymentProvider provider)
			throws Exception {
		// Check if purchase already exists for idempotency
		if (purchaseRepository.existsByPaymentIntentId(paymentIntentId)) {
			throw new Exception("Purchase already processed for Payment ID: " + paymentIntentId);
		}

		// Retrieve user and credit package
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new Exception("User not found with ID: " + userId));

		CreditPackage creditPackage = creditPackageRepository.findById(creditPackageId)
				.orElseThrow(() -> new Exception("Credit Package not found with ID: " + creditPackageId));

		// Verify payment based on provider
		boolean paymentVerified = verifyPayment(paymentIntentId, provider);
		if (!paymentVerified) {
			throw new Exception("Payment verification failed for " + provider + " payment: " + paymentIntentId);
		}

		// Update user wallet balance
		userWalletService.addCredits(userId, creditPackage.getCreditAmount());

		// Create Purchase record
		Purchase purchase = new Purchase();
		purchase.setUser(user);
		purchase.setCreditPackage(creditPackage);
		purchase.setAmount(BigDecimal.valueOf(creditPackage.getPrice()).setScale(2, RoundingMode.HALF_UP));
		purchase.setPurchaseDate(LocalDateTime.now());
		purchase.setPaymentIntentId(paymentIntentId);
		purchase.setPaymentProvider(provider);
		purchase.setStatus(PaymentStatus.COMPLETED);
		purchase.setCurrency("USD");
		purchase.setCreditsPurchased(creditPackage.getCreditAmount());

		purchaseRepository.save(purchase);

		String message = "Payment successful! " + creditPackage.getCreditAmount()
				+ " credits have been added to your account.";
		notificationService.createNotification(user, message, NotificationEntityType.PAYMENT, purchase.getId());
	}

	private boolean verifyPayment(String paymentIntentId, PaymentProvider provider) throws Exception {
		switch (provider) {
			case STRIPE:
				return verifyStripePayment(paymentIntentId);
			case PAYPAL:
				// For PayPal, since react-paypal-js handles the flow client-side
				return paymentIntentId != null && !paymentIntentId.isBlank();
			default:
				throw new IllegalArgumentException("Unsupported payment provider: " + provider);
		}
	}

	private boolean verifyStripePayment(String paymentIntentId) throws Exception {
		try {
			PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
			return "succeeded".equals(paymentIntent.getStatus());
		} catch (StripeException e) {
			throw new Exception("Failed to verify Stripe payment: " + e.getMessage(), e);
		}
	}

	@Override
	public Order createPaypalOrder(UUID userId, Long creditPackageId) throws IOException, ApiException {
		// Fetch credit package from database
		CreditPackage creditPackage = creditPackageRepository.findById(creditPackageId)
				.orElseThrow(() -> new RuntimeException("Credit Package not found with ID: " + creditPackageId));

		// Convert price to string for PayPal (ensure 2 decimal places)
		String priceString = String.format("%.2f", creditPackage.getPrice());

		CreateOrderInput createOrderInput = new CreateOrderInput.Builder(null, new OrderRequest.Builder(
				CheckoutPaymentIntent.fromString("CAPTURE"),
				Arrays.asList(new PurchaseUnitRequest.Builder(new AmountWithBreakdown.Builder("USD", priceString)
						.breakdown(new AmountBreakdown.Builder().itemTotal(new Money("USD", priceString)).build())
						.build())
						.items(
								Arrays.asList(new Item.Builder(creditPackage.getName(),
										new Money.Builder("USD", priceString).build(), "1")
										.description(creditPackage.getCreditAmount() + " credits package")
										.sku("CREDIT_PKG_" + creditPackageId)
										.category(ItemCategory.DIGITAL_GOODS).build()))
						.build()))
				.build()).build();

		OrdersController ordersController = client.getOrdersController();
		ApiResponse<Order> apiResponse = ordersController.createOrder(createOrderInput);
		return apiResponse.getResult();
	}

	@Override
	public Order capturePaypalOrders(String orderID) throws IOException, ApiException {
		CaptureOrderInput ordersCaptureInput = new CaptureOrderInput.Builder(orderID, null).build();
		OrdersController ordersController = client.getOrdersController();
		ApiResponse<Order> apiResponse = ordersController.captureOrder(ordersCaptureInput);
		return apiResponse.getResult();
	}
}
