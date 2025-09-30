package com.nix.service;

import java.math.BigDecimal;

/**
 * Minimal service for creating PayPal payouts. Note: In production, use PayPal
 * Payouts API SDK or REST calls with OAuth2.
 */
public interface PayPalPayoutService {
	/**
	 * Create a payout to a PayPal account email.
	 * 
	 * @param paypalEmail recipient PayPal email
	 * @param amount      USD amount
	 * @param currency    ISO currency code (e.g., USD)
	 * @param note        optional note/description
	 * @return provider payout id
	 * @throws Exception on errors
	 */
	String createPayout(String paypalEmail, BigDecimal amount, String currency, String note) throws Exception;

	/**
	 * Get the payout batch status by the payout_batch_id returned when creating a
	 * payout. Returns one of SUCCESS, PENDING, PROCESSING, FAILED, or UNKNOWN based
	 * on provider response.
	 */
	PayPalPayoutStatus getPayoutBatchStatus(String payoutBatchId) throws Exception;

	enum PayPalPayoutStatus {
		SUCCESS, PENDING, PROCESSING, FAILED, UNKNOWN
	}
}
