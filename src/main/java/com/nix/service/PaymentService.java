package com.nix.service;

import java.util.List;
import java.util.UUID;

import com.nix.models.CreditPackage;

public interface PaymentService {
	String createPaymentIntent(long amount, String currency, UUID userId, Long creditPackageId) throws Exception;

	List<CreditPackage> getAllActiveCreditPackages();

	CreditPackage getCreditPackageById(Long id);

	void confirmPayment(UUID userId, Long creditPackageId, String paymentIntentId) throws Exception;

}
