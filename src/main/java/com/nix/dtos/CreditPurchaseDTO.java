package com.nix.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreditPurchaseDTO {
	private Integer amount; // Number of credits to purchase
    private String paymentMethodId; // Stripe Payment Method ID

}
