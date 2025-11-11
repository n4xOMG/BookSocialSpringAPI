package com.nix.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreditPackageDTO {
	private Long id;
	private String name;
	private int creditAmount;
	private double price;
	private boolean isActive; // return active
	private double pricePerCredit;
	private String currency;
}
