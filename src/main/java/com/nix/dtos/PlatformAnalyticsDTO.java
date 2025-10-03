package com.nix.dtos;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlatformAnalyticsDTO {
	private BigDecimal platformFeeEarnings;
	private BigDecimal authorEarnings;
	private Long totalPayouts;
	private BigDecimal pendingPayouts;
	private Long totalReports;
	private Long pendingReports;
}
