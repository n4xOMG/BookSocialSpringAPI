package com.nix.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.nix.enums.PaymentProvider;
import com.nix.models.CreditPackage;
import com.paypal.sdk.exceptions.ApiException;
import com.paypal.sdk.models.Order;

public interface PaymentService {

	/**
	 * Creates a Stripe payment intent only. For PayPal, the react-paypal-js package
	 * handles order creation client-side.
	 */
	String createStripePaymentIntent(long amount, String currency, UUID userId, Long creditPackageId) throws Exception;

	/**
	 * Creates a Paypal payment intent only.
	 */
	public Order createPaypalOrder(UUID userId, Long creditPackageId) throws IOException, ApiException;

	/**
	 * Get PayPal order details (includes customId for verification)
	 */
	public Order getPaypalOrder(String orderID) throws IOException, ApiException;

	/**
	 * Capture Paypal payment intent only.
	 */
	public Order capturePaypalOrders(String orderID) throws IOException, ApiException;

	/**
	 * Confirms payment and updates user credits for both Stripe and PayPal
	 */
	void confirmPayment(UUID userId, Long creditPackageId, String paymentIntentId, PaymentProvider provider)
			throws Exception;

	/**
	 * Legacy method for backward compatibility
	 */
	default String createPayment(long amount, String currency, UUID userId, Long creditPackageId,
			PaymentProvider provider) throws Exception {
		if (provider == PaymentProvider.STRIPE) {
			return createStripePaymentIntent(amount, currency, userId, creditPackageId);
		} else {
			throw new UnsupportedOperationException("PayPal orders are created client-side using react-paypal-js");
		}
	}

	List<CreditPackage> getAllActiveCreditPackages();

	CreditPackage getCreditPackageById(Long id);
}
