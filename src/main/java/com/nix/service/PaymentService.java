package com.nix.service;

import java.util.List;

import com.nix.models.CreditPackage;

public interface PaymentService {
	String createPaymentIntent(long amount, String currency, Integer userId, Long creditPackageId) throws Exception;

	List<CreditPackage> getAllActiveCreditPackages();

	CreditPackage getCreditPackageById(Long id);

	void confirmPayment(Integer userId, Long creditPackageId, String paymentIntentId) throws Exception;

}
