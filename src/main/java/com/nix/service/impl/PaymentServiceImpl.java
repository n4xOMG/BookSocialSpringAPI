package com.nix.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
import com.paypal.sdk.models.OrderStatus;
import com.paypal.sdk.models.OrdersCapture;
import com.paypal.sdk.models.PurchaseUnit;
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

		String normalizedCurrency = (currency == null || currency.isBlank()) ? "usd"
				: currency.toLowerCase(Locale.ROOT);

		PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
				.setAmount(amount)
				.setCurrency(normalizedCurrency)
				.putMetadata("userId", userId.toString())
				.putMetadata("creditPackageId", String.valueOf(creditPackageId))
				.putMetadata("currency", normalizedCurrency)
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
		if (creditPackageId == null) {
			throw new IllegalArgumentException("Credit package identifier is required.");
		}
		if (paymentIntentId == null || paymentIntentId.isBlank()) {
			throw new IllegalArgumentException("Payment identifier is required.");
		}

		if (purchaseRepository.existsByPaymentIntentId(paymentIntentId)) {
			throw new Exception("Purchase already processed for Payment ID: " + paymentIntentId);
		}

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new Exception("User not found with ID: " + userId));

		CreditPackage creditPackage = creditPackageRepository.findById(creditPackageId)
				.orElseThrow(() -> new Exception("Credit Package not found with ID: " + creditPackageId));

		switch (provider) {
			case STRIPE -> validateStripePayment(paymentIntentId, user, creditPackage);
			case PAYPAL -> validatePaypalPayment(paymentIntentId, user, creditPackage);
			default -> throw new IllegalArgumentException("Unsupported payment provider: " + provider);
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

	private void validateStripePayment(String paymentIntentId, User user, CreditPackage creditPackage)
			throws Exception {
		PaymentIntent paymentIntent;
		try {
			paymentIntent = PaymentIntent.retrieve(paymentIntentId);
		} catch (StripeException e) {
			throw new Exception("Failed to retrieve Stripe payment intent: " + e.getMessage(), e);
		}

		if (!"succeeded".equalsIgnoreCase(paymentIntent.getStatus())) {
			throw new Exception("Stripe payment intent is not completed.");
		}

		Map<String, String> metadata = paymentIntent.getMetadata();
		if (metadata == null || metadata.isEmpty()) {
			throw new Exception("Stripe payment is missing verification metadata.");
		}

		String metadataUserId = metadata.get("userId");
		if (metadataUserId == null || !metadataUserId.equals(user.getId().toString())) {
			throw new Exception("Stripe payment does not belong to the requesting user.");
		}

		String metadataPackageId = metadata.get("creditPackageId");
		if (metadataPackageId == null
				|| !Objects.equals(metadataPackageId, String.valueOf(creditPackage.getId()))) {
			throw new Exception("Stripe payment does not match the requested credit package.");
		}

		BigDecimal expectedAmount = BigDecimal.valueOf(creditPackage.getPrice())
				.setScale(2, RoundingMode.HALF_UP);
		long expectedCents = expectedAmount.multiply(BigDecimal.valueOf(100)).longValueExact();

		Long amountReceived = paymentIntent.getAmountReceived();
		Long declaredAmount = paymentIntent.getAmount();
		long actualCents = (amountReceived != null && amountReceived > 0) ? amountReceived
				: (declaredAmount != null ? declaredAmount : -1);
		if (actualCents != expectedCents) {
			throw new Exception("Stripe payment amount mismatch.");
		}

		String currency = paymentIntent.getCurrency();
		String metadataCurrency = metadata.get("currency");
		if (currency == null || currency.isBlank()) {
			throw new Exception("Stripe payment currency is missing.");
		}
		if (metadataCurrency != null && !currency.equalsIgnoreCase(metadataCurrency)) {
			throw new Exception("Stripe payment currency mismatch.");
		}
		if (!"usd".equalsIgnoreCase(currency)) {
			throw new Exception("Unsupported Stripe currency: " + currency);
		}
	}

	private void validatePaypalPayment(String orderId, User user, CreditPackage creditPackage) throws Exception {
		Order order;
		try {
			order = capturePaypalOrders(orderId);
		} catch (ApiException | IOException e) {
			throw new Exception("Failed to capture PayPal order: " + e.getMessage(), e);
		}

		if (order == null) {
			throw new Exception("PayPal order could not be retrieved.");
		}
		if (order.getStatus() != OrderStatus.COMPLETED) {
			throw new Exception("PayPal order is not completed.");
		}
		if (order.getPurchaseUnits() == null || order.getPurchaseUnits().isEmpty()) {
			throw new Exception("PayPal order does not contain purchase units.");
		}

		PurchaseUnit purchaseUnit = order.getPurchaseUnits().get(0);
		PaypalAmountDetails amountDetails = resolvePaypalAmountDetails(purchaseUnit);
		BigDecimal orderTotal = amountDetails.amount();
		BigDecimal expectedTotal = BigDecimal.valueOf(creditPackage.getPrice()).setScale(2, RoundingMode.HALF_UP);
		if (!orderTotal.equals(expectedTotal)) {
			throw new Exception("PayPal order amount mismatch.");
		}

		String currencyCode = amountDetails.currencyCode();
		if (currencyCode == null || currencyCode.isBlank()) {
			throw new Exception("PayPal order currency is missing.");
		}
		if (!"USD".equalsIgnoreCase(currencyCode)) {
			throw new Exception("Unsupported PayPal currency: " + currencyCode);
		}

		String customId = purchaseUnit.getCustomId();
		if (customId != null && !customId.isBlank() && !customId.equals(user.getId().toString())) {
			throw new Exception("PayPal order does not belong to the requesting user.");
		}

		if (purchaseUnit.getItems() != null && !purchaseUnit.getItems().isEmpty()) {
			Item item = purchaseUnit.getItems().get(0);
			String expectedSku = "CREDIT_PKG_" + creditPackage.getId();
			if (item.getSku() != null && !expectedSku.equals(item.getSku())) {
				throw new Exception("PayPal order SKU mismatch.");
			}
		}
	}

	private PaypalAmountDetails resolvePaypalAmountDetails(PurchaseUnit purchaseUnit) throws Exception {
		AmountWithBreakdown amountWithBreakdown = purchaseUnit.getAmount();
		if (amountWithBreakdown != null && amountWithBreakdown.getValue() != null) {
			return new PaypalAmountDetails(parsePaypalAmount(amountWithBreakdown.getValue()),
					sanitizeCurrency(amountWithBreakdown.getCurrencyCode()));
		}

		if (purchaseUnit.getPayments() != null && purchaseUnit.getPayments().getCaptures() != null) {
			for (OrdersCapture capture : purchaseUnit.getPayments().getCaptures()) {
				if (capture == null || capture.getAmount() == null || capture.getAmount().getValue() == null) {
					continue;
				}
				return new PaypalAmountDetails(parsePaypalAmount(capture.getAmount().getValue()),
						sanitizeCurrency(capture.getAmount().getCurrencyCode()));
			}
		}

		throw new Exception("PayPal order amount is missing.");
	}

	private BigDecimal parsePaypalAmount(String rawAmount) throws Exception {
		try {
			return new BigDecimal(rawAmount).setScale(2, RoundingMode.HALF_UP);
		} catch (NumberFormatException ex) {
			throw new Exception("Invalid PayPal order amount format.", ex);
		}
	}

	private String sanitizeCurrency(String currencyCode) {
		return currencyCode == null ? null : currencyCode.trim();
	}

	private record PaypalAmountDetails(BigDecimal amount, String currencyCode) {
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
						.customId(userId.toString())
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
	public Order getPaypalOrder(String orderID) throws IOException, ApiException {
		OrdersController ordersController = client.getOrdersController();
		ApiResponse<Order> apiResponse = ordersController.getOrder(
				new com.paypal.sdk.models.GetOrderInput.Builder(orderID).build());
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
