package com.nix.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nix.models.User;
import com.nix.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

import jakarta.annotation.PostConstruct;

@Service
public class PaymentServiceImpl implements PaymentService {
	@Value("${stripe.apiKey}")
	private String stripeApiKey;

	@Autowired
	private UserRepository userRepository;

	@PostConstruct
	public void init() {
		Stripe.apiKey = stripeApiKey;
	}

	@Override
	public PaymentIntent createPaymentIntent(Long amount, String currency) throws StripeException {
		PaymentIntentCreateParams params = PaymentIntentCreateParams.builder().setAmount(amount).setCurrency(currency)
				.build();

		return PaymentIntent.create(params);
	}

	@Override
	public void handleSuccessfulPayment(Integer userId, Integer creditsPurchased) throws Exception {
		User user = userRepository.findById(userId).orElseThrow(() -> new Exception("User not found"));

		user.setCredits(user.getCredits() + creditsPurchased);
		userRepository.save(user);
	}

}
