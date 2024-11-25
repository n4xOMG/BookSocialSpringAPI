package com.nix.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nix.models.CreditPackage;
import com.nix.models.Purchase;
import com.nix.models.User;
import com.nix.repository.CreditPackageRepository;
import com.nix.repository.PurchaseRepository;
import com.nix.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class PaymentServiceImpl implements PaymentService {
	@Value("${stripe.apiKey}")
	private String stripeApiKey;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CreditPackageRepository creditPackageRepository;

	@Autowired
	private PurchaseRepository purchaseRepository;

	@PostConstruct
	public void init() {
		com.stripe.Stripe.apiKey = stripeApiKey;
	}

	@Override
	public String createPaymentIntent(long amount, String currency, Integer userId, Long creditPackageId)
			throws StripeException {
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
	public void confirmPayment(Integer userId, Long creditPackageId, String paymentIntentId) throws Exception {
		// Check if purchase already exists for idempotency
		if (purchaseRepository.existsByPaymentIntentId(paymentIntentId)) {
			throw new Exception("Purchase already processed for PaymentIntent ID: " + paymentIntentId);
		}

		// Retrieve user and credit package
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new Exception("User not found with ID: " + userId));

		CreditPackage creditPackage = creditPackageRepository.findById(creditPackageId)
				.orElseThrow(() -> new Exception("Credit Package not found with ID: " + creditPackageId));

		// Update user credits
		user.setCredits(user.getCredits() + creditPackage.getCreditAmount());
		userRepository.save(user);

		// Create Purchase record
		Purchase purchase = new Purchase();
		purchase.setUser(user);
		purchase.setCreditPackage(creditPackage);
		purchase.setAmount(creditPackage.getCreditAmount());
		purchase.setPurchaseDate(LocalDateTime.now());
		purchase.setPaymentIntentId(paymentIntentId);

		purchaseRepository.save(purchase);
	}

}
