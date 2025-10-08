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
public class AuthorDashboardDTO {
	// Overall earnings summary
	private BigDecimal totalLifetimeEarnings;
	private BigDecimal totalUnpaidEarnings;
	private BigDecimal totalPaidOut;
	private BigDecimal currentBalance; // Same as totalUnpaidEarnings

	// Current period earnings (this month)
	private BigDecimal currentMonthEarnings;
	private BigDecimal lastMonthEarnings;
	private BigDecimal earningsGrowthPercentage;

	// Book performance overview
	private long totalBooks;
	private long totalChapters;
	private long totalViews;
	private long totalLikes;
	private long totalComments;
	private BigDecimal averageRating;

	// Recent activity
	private List<AuthorEarningDTO> recentEarnings;
	private List<AuthorPayoutDTO> recentPayouts;
	private List<BookPerformanceDTO> topPerformingBooks;

	// Payout information
	private boolean canRequestPayout;
	private BigDecimal minimumPayoutAmount;
	private String nextScheduledPayoutDate;
	private boolean payoutMethodConfigured; // true if PayPal email is set
}