package com.nix.request;

import com.nix.enums.PaymentProvider;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseRequest {
	private Long creditPackageId;
	private String currency;
	private PaymentProvider paymentProvider; // Optional - defaults to STRIPE for legacy support
}
