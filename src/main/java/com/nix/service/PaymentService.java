package com.nix.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

public interface PaymentService {
	public PaymentIntent createPaymentIntent(Long amount, String currency) throws StripeException;
	
	public void handleSuccessfulPayment(Integer userId, Integer creditsPurchased) throws Exception;
}
