package com.nix.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.nix.enums.PaymentProvider;
import com.nix.enums.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseDTO {
	private UUID id;
	private BigDecimal amount;
	private LocalDateTime purchaseDate;
	private String paymentIntentId;
	private CreditPackageDTO creditPackage;
	private PaymentProvider paymentProvider;
	private PaymentStatus status;
	private String currency;
	private int creditsPurchased;
}
