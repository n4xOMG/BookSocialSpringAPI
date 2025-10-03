package com.nix.dtos;

import java.math.BigDecimal;

import com.nix.enums.PaymentProvider;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProviderStatsDTO {
	private PaymentProvider provider;
	private Long transactionCount;
	private BigDecimal totalRevenue;
	private Double percentage;
}