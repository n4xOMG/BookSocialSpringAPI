package com.nix.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.nix.dtos.ActiveUserAnalyticsDTO;
import com.nix.dtos.BestBookAnalyticsDTO;
import com.nix.dtos.CategoryStatsDTO;
import com.nix.dtos.ContentAnalyticsDTO;
import com.nix.dtos.DailyRevenueDTO;
import com.nix.dtos.PaymentProviderStatsDTO;
import com.nix.dtos.PlatformAnalyticsDTO;
import com.nix.dtos.PopularAuthorDTO;
import com.nix.dtos.PopularBookDTO;
import com.nix.dtos.PopularChapterDTO;
import com.nix.dtos.RevenueAnalyticsDTO;
import com.nix.dtos.TopSpenderAnalyticsDTO;
import com.nix.dtos.UserAnalyticsDTO;
import com.nix.dtos.UserGrowthDTO;
import com.nix.enums.PaymentProvider;
import com.nix.models.Book;
import com.nix.models.Chapter;
import com.nix.models.User;
import com.nix.repository.AuthorEarningRepository;
import com.nix.repository.AuthorPayoutRepository;
import com.nix.repository.BookFavouriteRepository;
import com.nix.repository.BookRepository;
import com.nix.repository.BookViewHistoryRepository;
import com.nix.repository.ChapterRepository;
import com.nix.repository.CommentRepository;
import com.nix.repository.PurchaseRepository;
import com.nix.repository.ReadingProgressRepository;
import com.nix.repository.ReportRepository;
import com.nix.repository.UserRepository;
import com.nix.service.AdminService;

@Service
public class AdminServiceImpl implements AdminService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PurchaseRepository purchaseRepository;

	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private BookFavouriteRepository bookFavouriteRepository;

	@Autowired
	private ChapterRepository chapterRepository;

	@Autowired
	private AuthorEarningRepository authorEarningRepository;

	@Autowired
	private AuthorPayoutRepository authorPayoutRepository;

	@Autowired
	private ReportRepository reportRepository;

	@Autowired
	private BookViewHistoryRepository bookViewHistoryRepository;

	@Autowired
	private CommentRepository commentRepository;

	@Autowired
	private ReadingProgressRepository readingProgressRepository;

	@Override
	public UserAnalyticsDTO getUserAnalytics() {
		LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
		LocalDateTime thisMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

		UserAnalyticsDTO userAnalytics = new UserAnalyticsDTO();
		userAnalytics.setTotalUsers(userRepository.count());
		userAnalytics.setActiveUsers(userRepository.countActiveUsersFromDate(thirtyDaysAgo));
		userAnalytics.setNewUsersThisMonth(userRepository.countUsersFromDate(thisMonth));
		userAnalytics.setBannedUsers(userRepository.countBannedUsers());
		userAnalytics.setSuspendedUsers(userRepository.countSuspendedUsers());

		// Get user growth history for last 30 days
		long totalUsers = userAnalytics.getTotalUsers();
		List<Object[]> growthData = userRepository.getUserGrowthData(thirtyDaysAgo);
		List<UserGrowthDTO> userGrowthHistory = growthData.stream().map(data -> {
			Object dateRaw = data[0];
			LocalDate date;
			if (dateRaw instanceof LocalDate) {
				date = (LocalDate) dateRaw;
			} else if (dateRaw instanceof java.sql.Date sqlDate) {
				date = sqlDate.toLocalDate();
			} else if (dateRaw instanceof LocalDateTime ldt) {
				date = ldt.toLocalDate();
			} else {
				throw new IllegalStateException("Unsupported date type: " + dateRaw);
			}

			Long count = ((Number) data[1]).longValue();
			return new UserGrowthDTO(date, count, totalUsers);
		}).collect(Collectors.toList());
		userAnalytics.setUserGrowthHistory(userGrowthHistory);

		return userAnalytics;
	}

	@Override
	public RevenueAnalyticsDTO getRevenueAnalytics() {
		LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
		LocalDateTime weekAgo = today.minusDays(7);
		LocalDateTime monthAgo = today.minusDays(30);

		RevenueAnalyticsDTO revenueAnalytics = new RevenueAnalyticsDTO();

		// Basic revenue metrics
		BigDecimal totalRev = purchaseRepository.getTotalRevenue();
		BigDecimal monthlyRev = purchaseRepository.getRevenueFromDate(monthAgo);
		BigDecimal weeklyRev = purchaseRepository.getRevenueFromDate(weekAgo);
		BigDecimal dailyRev = purchaseRepository.getRevenueFromDate(today);
		BigDecimal avgOrderValue = purchaseRepository.getAverageOrderValue();

		revenueAnalytics.setTotalRevenue(totalRev != null ? totalRev : BigDecimal.ZERO);
		revenueAnalytics.setMonthlyRevenue(monthlyRev != null ? monthlyRev : BigDecimal.ZERO);
		revenueAnalytics.setWeeklyRevenue(weeklyRev != null ? weeklyRev : BigDecimal.ZERO);
		revenueAnalytics.setDailyRevenue(dailyRev != null ? dailyRev : BigDecimal.ZERO);
		revenueAnalytics.setAverageOrderValue(avgOrderValue != null ? avgOrderValue : BigDecimal.ZERO);
		revenueAnalytics.setTotalTransactions(purchaseRepository.getTotalCompletedPurchases());

		// Daily revenue history for last 30 days
		List<Object[]> dailyRevenueData = purchaseRepository.getDailyRevenue(monthAgo);
		List<DailyRevenueDTO> dailyRevenueHistory = dailyRevenueData.stream().map(data -> {
			Object dateRaw = data[0];
			LocalDate date;
			if (dateRaw instanceof LocalDate) {
				date = (LocalDate) dateRaw;
			} else if (dateRaw instanceof java.sql.Date sqlDate) {
				date = sqlDate.toLocalDate();
			} else if (dateRaw instanceof LocalDateTime ldt) {
				date = ldt.toLocalDate();
			} else {
				throw new IllegalStateException("Unsupported date type: " + dateRaw);
			}

			BigDecimal revenue = data[1] instanceof BigDecimal ? (BigDecimal) data[1]
					: BigDecimal.valueOf(((Number) data[1]).doubleValue());
			return new DailyRevenueDTO(date, revenue, 0L);
		}).collect(Collectors.toList());
		revenueAnalytics.setDailyRevenueHistory(dailyRevenueHistory);

		// Payment provider statistics
		List<Object[]> providerStats = purchaseRepository.getPaymentProviderStats();
		Long totalTransactions = revenueAnalytics.getTotalTransactions();
		BigDecimal totalRevenueBD = revenueAnalytics.getTotalRevenue();

		List<PaymentProviderStatsDTO> paymentProviderStats = providerStats.stream().map(data -> {
			PaymentProvider provider = (PaymentProvider) data[0];
			Long count = (Long) data[1];
			Double percentage = totalTransactions > 0 ? (count.doubleValue() / totalTransactions) * 100 : 0.0;

			BigDecimal providerRevenue = totalRevenueBD.multiply(BigDecimal.valueOf(percentage / 100.0)).setScale(2,
					RoundingMode.HALF_UP);

			return new PaymentProviderStatsDTO(provider, count, providerRevenue, percentage);
		}).collect(Collectors.toList());
		revenueAnalytics.setPaymentProviderStats(paymentProviderStats);

		return revenueAnalytics;
	}

	@Override
	public ContentAnalyticsDTO getContentAnalytics() {
		ContentAnalyticsDTO contentAnalytics = new ContentAnalyticsDTO();

		contentAnalytics.setTotalBooks(bookRepository.count());
		contentAnalytics.setTotalChapters(chapterRepository.count());
		contentAnalytics.setTotalUnlocks(chapterRepository.getTotalUnlocks());

		Pageable topLimit = PageRequest.of(0, 10);
		List<Book> mostViewedBooks = bookRepository.findMostViewedBooks(topLimit);
		List<PopularBookDTO> popularBooks = mostViewedBooks.stream()
				.map(book -> new PopularBookDTO(book.getId(), book.getTitle(), book.getAuthorName(),
						book.getViewCount(), 0L,
						0.0,
						bookFavouriteRepository.countByBookId(book.getId())))
				.collect(Collectors.toList());
		contentAnalytics.setPopularBooks(popularBooks);

		List<Chapter> mostUnlockedChapters = chapterRepository.findMostUnlockedChapters(topLimit);
		List<PopularChapterDTO> popularChapters = mostUnlockedChapters.stream()
				.map(chapter -> new PopularChapterDTO(chapter.getId(), chapter.getTitle(), chapter.getBook().getTitle(),
						chapter.getBook().getAuthorName(), (long) chapter.getUnlockRecords().size(),
						chapter.getPrice()))
				.collect(Collectors.toList());
		contentAnalytics.setPopularChapters(popularChapters);

		List<Object[]> topAuthorsData = authorEarningRepository.getTopEarningAuthors(topLimit);
		List<PopularAuthorDTO> popularAuthors = topAuthorsData.stream().map(data -> {
			User author = (User) data[0];
			BigDecimal earnings = (BigDecimal) data[1];

			return new PopularAuthorDTO(author.getId(), author.getUsername(), author.getFullname(), 0L, 0L, 0L,
					earnings);
		}).collect(Collectors.toList());
		contentAnalytics.setPopularAuthors(popularAuthors);

		List<Object[]> categoryStatsData = bookRepository.getCategoryStats();
		long totalBooksCount = bookRepository.count();
		List<CategoryStatsDTO> categoryStats = categoryStatsData.stream().map(data -> {
			String categoryName = (String) data[0];
			Long bookCount = (Long) data[1];
			Double percentage = totalBooksCount > 0 ? (bookCount.doubleValue() / totalBooksCount) * 100 : 0.0;

			return new CategoryStatsDTO(categoryName, bookCount, 0L, percentage);
		}).collect(Collectors.toList());
		contentAnalytics.setCategoryStats(categoryStats);

		return contentAnalytics;
	}

	@Override
	public PlatformAnalyticsDTO getPlatformAnalytics() {
		PlatformAnalyticsDTO platformAnalytics = new PlatformAnalyticsDTO();

		BigDecimal platformFeeEarnings = authorEarningRepository.getTotalPlatformFees();
		BigDecimal authorEarnings = authorEarningRepository.getTotalAuthorEarnings();
		Long totalPayouts = authorPayoutRepository.count();
		BigDecimal pendingPayouts = authorPayoutRepository.getTotalPendingPayoutAmount();
		Long totalReports = reportRepository.count();
		Long pendingReports = 0L;

		platformAnalytics.setPlatformFeeEarnings(platformFeeEarnings != null ? platformFeeEarnings : BigDecimal.ZERO);
		platformAnalytics.setAuthorEarnings(authorEarnings != null ? authorEarnings : BigDecimal.ZERO);
		platformAnalytics.setTotalPayouts(totalPayouts);
		platformAnalytics.setPendingPayouts(pendingPayouts != null ? pendingPayouts : BigDecimal.ZERO);
		platformAnalytics.setTotalReports(totalReports);
		platformAnalytics.setPendingReports(pendingReports);

		return platformAnalytics;
	}

	@Override
	public List<BestBookAnalyticsDTO> getBestBooks(String period, int limit) {
		LocalDateTime startDate = calculateStartDate(period);
		Pageable pageable = PageRequest.of(0, limit);

		List<Object[]> topBooksData = bookViewHistoryRepository.findTopBooksByViewsInPeriod(startDate, pageable);

		return topBooksData.stream().map(data -> {
			Book book = (Book) data[0];
			Long viewCount = ((Number) data[1]).longValue();
			Long favouriteCount = bookFavouriteRepository.countByBookId(book.getId());

			return new BestBookAnalyticsDTO(
					book.getId(),
					book.getTitle(),
					book.getAuthorName(),
					book.getBookCover() != null ? book.getBookCover().getUrl() : null,
					viewCount,
					favouriteCount,
					0.0);
		}).collect(Collectors.toList());
	}

	@Override
	public List<ActiveUserAnalyticsDTO> getMostActiveUsers(String period, int limit) {
		LocalDateTime startDate = calculateStartDate(period);
		Pageable pageable = PageRequest.of(0, limit);

		List<Object[]> topCommenters = commentRepository.findTopCommentingUsersInPeriod(startDate, pageable);
		List<Object[]> topReaders = readingProgressRepository.findMostActiveReadersInPeriod(startDate, pageable);

		java.util.Map<java.util.UUID, ActiveUserAnalyticsDTO> userActivityMap = new java.util.HashMap<>();

		for (Object[] data : topCommenters) {
			User user = (User) data[0];
			Long commentCount = ((Number) data[1]).longValue();

			ActiveUserAnalyticsDTO dto = userActivityMap.computeIfAbsent(user.getId(), k -> new ActiveUserAnalyticsDTO(
					user.getId(),
					user.getUsername(),
					user.getFullname(),
					user.getAvatarUrl(),
					0L, 0L, 0L));
			dto.setCommentCount(commentCount);
		}

		for (Object[] data : topReaders) {
			User user = (User) data[0];
			Long readCount = ((Number) data[1]).longValue();

			ActiveUserAnalyticsDTO dto = userActivityMap.computeIfAbsent(user.getId(), k -> new ActiveUserAnalyticsDTO(
					user.getId(),
					user.getUsername(),
					user.getFullname(),
					user.getAvatarUrl(),
					0L, 0L, 0L));
			dto.setReadCount(readCount);
		}

		return userActivityMap.values().stream()
				.peek(dto -> dto.setActivityScore(dto.getCommentCount() * 3 + dto.getReadCount()))
				.sorted((a, b) -> Long.compare(b.getActivityScore(), a.getActivityScore()))
				.limit(limit)
				.collect(Collectors.toList());
	}

	@Override
	public List<TopSpenderAnalyticsDTO> getTopSpenders(String period, int limit) {
		LocalDateTime startDate = calculateStartDate(period);
		Pageable pageable = PageRequest.of(0, limit);

		List<Object[]> topSpendersData = purchaseRepository.findTopSpendingUsersInPeriod(startDate, pageable);

		return topSpendersData.stream().map(data -> {
			User user = (User) data[0];
			BigDecimal totalSpent = data[1] instanceof BigDecimal ? (BigDecimal) data[1]
					: BigDecimal.valueOf(((Number) data[1]).doubleValue());
			Long transactionCount = ((Number) data[2]).longValue();
			Long creditsPurchased = ((Number) data[3]).longValue();

			return new TopSpenderAnalyticsDTO(
					user.getId(),
					user.getUsername(),
					user.getFullname(),
					user.getAvatarUrl(),
					totalSpent,
					transactionCount,
					creditsPurchased);
		}).collect(Collectors.toList());
	}

	private LocalDateTime calculateStartDate(String period) {
		LocalDateTime now = LocalDateTime.now();
		return switch (period.toLowerCase()) {
			case "day" -> now.minusDays(1);
			case "week" -> now.minusWeeks(1);
			case "month" -> now.minusMonths(1);
			default -> now.minusWeeks(1); // Default to week
		};
	}
}