package com.nix.response;

import com.nix.enums.PaymentProvider;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponse {
	private String clientSecret; // For Stripe
	private String approvalUrl; // For PayPal
	private PaymentProvider provider;
	private Long packageId;
	private String status;
	private String message;

	// Constructor for successful payment creation
	public PaymentResponse(String paymentData, PaymentProvider provider, Long packageId) {
		this.provider = provider;
		this.packageId = packageId;
		this.status = "success";

		if (provider == PaymentProvider.STRIPE) {
			this.clientSecret = paymentData;
		} else if (provider == PaymentProvider.PAYPAL) {
			this.approvalUrl = paymentData;
		}
	}

	// Constructor for error responses
	public PaymentResponse(String message, String status) {
		this.message = message;
		this.status = status;
	}
}