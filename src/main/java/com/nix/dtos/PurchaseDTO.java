package com.nix.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

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
    private double amount;
    private LocalDateTime purchaseDate;
    private String paymentIntentId;
    private CreditPackageDTO creditPackage;
}
