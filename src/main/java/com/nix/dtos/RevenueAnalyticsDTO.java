package com.nix.dtos;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RevenueAnalyticsDTO {
	private BigDecimal totalRevenue;
	private BigDecimal monthlyRevenue;
	private BigDecimal weeklyRevenue;
	private BigDecimal dailyRevenue;
	private BigDecimal averageOrderValue;
	private Long totalTransactions;
	private List<DailyRevenueDTO> dailyRevenueHistory;
	private List<PaymentProviderStatsDTO> paymentProviderStats;
}