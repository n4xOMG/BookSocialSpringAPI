package com.nix.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nix.dtos.AuthorDashboardDTO;
import com.nix.dtos.AuthorEarningDTO;
import com.nix.dtos.AuthorPayoutDTO;
import com.nix.dtos.AuthorPayoutSettingsDTO;
import com.nix.dtos.BookPerformanceDTO;
import com.nix.dtos.mappers.AuthorEarningMapper;
import com.nix.dtos.mappers.AuthorPayoutMapper;
import com.nix.dtos.mappers.AuthorPayoutSettingsMapper;
import com.nix.models.AuthorEarning;
import com.nix.models.AuthorPayout;
import com.nix.models.AuthorPayoutSettings;
import com.nix.models.Book;
import com.nix.models.BookFavourite;
import com.nix.models.Chapter;
import com.nix.models.ChapterUnlockRecord;
import com.nix.models.User;
import com.nix.repository.AuthorEarningRepository;
import com.nix.repository.AuthorPayoutRepository;
import com.nix.repository.AuthorPayoutSettingsRepository;
import com.nix.repository.BookFavouriteRepository;
import com.nix.repository.BookRepository;
import com.nix.service.AuthorService;
import com.nix.service.BookService;
import com.nix.service.CreditPackageService;
import com.nix.service.PayPalPayoutService;

@Service
@Transactional
public class AuthorServiceImpl implements AuthorService {

	@Value("${app.platform.fee.percentage:7.5}")
	private BigDecimal platformFeePercentage;

	@Value("${frontend.url}")
	private String frontendUrl;

	@Autowired
	private PayPalPayoutService payPalPayoutService;

	@Value("${app.minimum.payout.amount:25.00}")
	private BigDecimal defaultMinimumPayoutAmount;

	@Autowired
	private AuthorEarningRepository authorEarningRepository;

	@Autowired
	private AuthorPayoutRepository authorPayoutRepository;

	@Autowired
	private AuthorPayoutSettingsRepository authorPayoutSettingsRepository;

	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private BookFavouriteRepository bookFavouriteRepository;

	@Autowired
	private BookService bookService;

	@Autowired
	private CreditPackageService creditPackageService;

	@Autowired
	private AuthorEarningMapper authorEarningMapper;

	@Autowired
	private AuthorPayoutMapper authorPayoutMapper;

	@Autowired
	private AuthorPayoutSettingsMapper authorPayoutSettingsMapper;

	// === EARNINGS METHODS ===

	@Override
	public void recordChapterUnlockEarning(ChapterUnlockRecord unlockRecord) {
		Chapter chapter = unlockRecord.getChapter();
		User author = chapter.getBook().getAuthor();

		BigDecimal usdPerCredit = creditPackageService.getCurrentUsdPricePerCredit();
		BigDecimal grossAmount = usdPerCredit.multiply(BigDecimal.valueOf(unlockRecord.getUnlockCost()))
				.setScale(2, RoundingMode.HALF_UP);
		if (grossAmount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalStateException("Calculated gross amount for unlock must be greater than zero");
		}

		AuthorEarning earning = new AuthorEarning(author, chapter, unlockRecord, grossAmount, platformFeePercentage);

		authorEarningRepository.save(earning);
	}

	@Override
	public AuthorDashboardDTO getAuthorDashboard(User author, Pageable pageable) {
		AuthorDashboardDTO dashboard = new AuthorDashboardDTO();

		// Earnings summary
		BigDecimal lifetimeEarnings = getLifetimeEarnings(author);
		BigDecimal totalPaidOut = authorPayoutRepository.getTotalPaidOutForAuthor(author);
		BigDecimal unpaidEarnings = lifetimeEarnings.subtract(totalPaidOut);

		dashboard.setTotalLifetimeEarnings(lifetimeEarnings);
		dashboard.setTotalUnpaidEarnings(unpaidEarnings);
		dashboard.setTotalPaidOut(totalPaidOut);
		dashboard.setCurrentBalance(unpaidEarnings);

		// Monthly earnings comparison
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
		LocalDateTime startOfLastMonth = startOfMonth.minusMonths(1);
		LocalDateTime endOfLastMonth = startOfMonth.minusSeconds(1);

		dashboard.setCurrentMonthEarnings(authorEarningRepository.getEarningsForPeriod(author, startOfMonth, now));
		dashboard.setLastMonthEarnings(
				authorEarningRepository.getEarningsForPeriod(author, startOfLastMonth, endOfLastMonth));

		// Calculate growth percentage
		if (dashboard.getLastMonthEarnings().compareTo(BigDecimal.ZERO) > 0) {
			BigDecimal growth = dashboard.getCurrentMonthEarnings().subtract(dashboard.getLastMonthEarnings())
					.divide(dashboard.getLastMonthEarnings(), 4, RoundingMode.HALF_UP)
					.multiply(BigDecimal.valueOf(100));
			dashboard.setEarningsGrowthPercentage(growth);
		} else {
			dashboard.setEarningsGrowthPercentage(BigDecimal.ZERO);
		}

		// Book overview
		Page<Book> authorBooks = bookRepository.findByAuthorId(author.getId(), pageable);
		dashboard.setTotalBooks(authorBooks.getTotalElements());
		dashboard.setTotalChapters(authorBooks.stream().mapToInt(book -> book.getChapters().size()).sum());

		// Calculate total views, likes, and comments across all books
		long totalViews = authorBooks.stream().mapToLong(Book::getViewCount).sum();
		long totalFavourites = authorBooks.stream()
				.mapToLong(book -> bookFavouriteRepository.countByBookId(book.getId())).sum();
		long totalComments = authorBooks.stream().mapToLong(book -> book.getComments().size()).sum();

		dashboard.setTotalViews(totalViews);
		dashboard.setTotalLikes(totalFavourites);
		dashboard.setTotalComments(totalComments);

		// Recent activity
		dashboard.setRecentEarnings(getAuthorEarnings(author, Pageable.ofSize(5)).getContent());

		List<AuthorPayout> recentPayouts = authorPayoutRepository
				.findByAuthorOrderByRequestedDateDesc(author, Pageable.ofSize(5)).getContent();
		dashboard.setRecentPayouts(authorPayoutMapper.mapToDTOs(recentPayouts));

		// Top performing books (limited to top 5)
		List<BookPerformanceDTO> allBookPerformance = bookService.getAuthorBookPerformance(author.getId());
		List<BookPerformanceDTO> topPerformingBooks = allBookPerformance.stream()
				.sorted((a, b) -> Long.compare(b.getDailyViewsGrowth() + b.getWeeklyViewsGrowth(),
						a.getDailyViewsGrowth() + a.getWeeklyViewsGrowth()))
				.limit(5)
				.toList();
		dashboard.setTopPerformingBooks(topPerformingBooks);

		// Payout settings
		AuthorPayoutSettings settings = getOrCreatePayoutSettingsEntity(author);
		dashboard.setMinimumPayoutAmount(settings.getMinimumPayoutAmount());
		dashboard.setPayoutMethodConfigured(settings.getPaypalEmail() != null && !settings.getPaypalEmail().isBlank());
		dashboard.setCanRequestPayout(canRequestPayout(author));

		return dashboard;
	}

	@Override
	public Page<AuthorEarningDTO> getAuthorEarnings(User author, Pageable pageable) {
		Page<AuthorEarning> earnings = authorEarningRepository.findByAuthorOrderByEarnedDateDesc(author, pageable);

		List<AuthorEarningDTO> dtos = authorEarningMapper.mapToDTOs(earnings.getContent());

		return new PageImpl<>(dtos, pageable, earnings.getTotalElements());
	}

	@Override
	public BigDecimal getUnpaidEarnings(User author) {
		BigDecimal lifetime = getLifetimeEarnings(author);
		BigDecimal paidOut = authorPayoutRepository.getTotalPaidOutForAuthor(author);
		return lifetime.subtract(paidOut);
	}

	@Override
	public BigDecimal getLifetimeEarnings(User author) {
		return authorEarningRepository.getTotalLifetimeEarningsForAuthor(author);
	}

	// === PAYOUT METHODS ===

	@Override
	public AuthorPayoutDTO requestPayout(User author, BigDecimal amount) throws Exception {
		if (!canRequestPayout(author)) {
			throw new Exception("Payout requirements not met");
		}

		BigDecimal availableBalance = getUnpaidEarnings(author);
		if (amount.compareTo(availableBalance) > 0) {
			throw new Exception("Requested amount exceeds available balance");
		}

		AuthorPayoutSettings settings = getOrCreatePayoutSettingsEntity(author);
		if (amount.compareTo(settings.getMinimumPayoutAmount()) < 0) {
			throw new Exception("Amount below minimum payout threshold");
		}

		// Get unpaid earnings
		List<AuthorEarning> unpaidEarnings = authorEarningRepository
				.findByAuthorAndIsPaidOutFalseOrderByEarnedDateDesc(author);

		BigDecimal totalPlatformFees = unpaidEarnings.stream().map(AuthorEarning::getPlatformFee)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal roundedAmount = amount.setScale(2, RoundingMode.HALF_UP);
		BigDecimal roundedFees = totalPlatformFees.setScale(2, RoundingMode.HALF_UP);

		AuthorPayout payout = new AuthorPayout(author, roundedAmount, roundedFees);
		AuthorPayout savedPayout = authorPayoutRepository.save(payout);

		// Mark earnings as paid out
		unpaidEarnings.forEach(earning -> {
			earning.setPaidOut(true);
			earning.setPayout(savedPayout);
		});
		authorEarningRepository.saveAll(unpaidEarnings);

		return authorPayoutMapper.mapToDTO(savedPayout);
	}

	@Override
	public Page<AuthorPayoutDTO> getAuthorPayouts(User author, Pageable pageable) {
		Page<AuthorPayout> payouts = authorPayoutRepository.findByAuthorOrderByRequestedDateDesc(author, pageable);

		List<AuthorPayoutDTO> dtos = authorPayoutMapper.mapToDTOs(payouts.getContent());

		return new PageImpl<>(dtos, pageable, payouts.getTotalElements());
	}

	@Override
	public AuthorPayoutSettingsDTO getPayoutSettings(User author) {
		AuthorPayoutSettings settings = getOrCreatePayoutSettingsEntity(author);
		return authorPayoutSettingsMapper.mapToDTO(settings);
	}

	@Override
	public AuthorPayoutSettingsDTO updatePayoutSettings(User author, AuthorPayoutSettings settings) {
		AuthorPayoutSettings existing = getOrCreatePayoutSettingsEntity(author);
		existing.setPaypalEmail(settings.getPaypalEmail());
		existing.setMinimumPayoutAmount(settings.getMinimumPayoutAmount());
		existing.setPayoutFrequency(settings.getPayoutFrequency());
		existing.setAutoPayoutEnabled(settings.isAutoPayoutEnabled());
		existing.setUpdatedDate(LocalDateTime.now());

		AuthorPayoutSettings saved = authorPayoutSettingsRepository.save(existing);
		return authorPayoutSettingsMapper.mapToDTO(saved);
	}

	@Override
	public boolean canRequestPayout(User author) {
		AuthorPayoutSettings settings = getOrCreatePayoutSettingsEntity(author);
		BigDecimal availableBalance = getUnpaidEarnings(author);
		boolean hasPayPal = settings.getPaypalEmail() != null && !settings.getPaypalEmail().isBlank();
		return hasPayPal && availableBalance.compareTo(settings.getMinimumPayoutAmount()) >= 0;
	}

	@Override
	public BigDecimal getPlatformFeePercentage() {
		return platformFeePercentage;
	}

	// === PAYPAL PROCESSING (called by scheduler) ===
	public void processPayPalPayout(AuthorPayout payout) throws Exception {
		AuthorPayoutSettings settings = authorPayoutSettingsRepository.findByAuthor(payout.getAuthor())
				.orElseThrow(() -> new Exception("Payout settings not found"));

		if (settings.getPaypalEmail() == null || settings.getPaypalEmail().isBlank()) {
			throw new Exception("PayPal email not configured for author");
		}

		BigDecimal payoutAmount = payout.getTotalAmount().setScale(2, RoundingMode.HALF_UP);
		String providerPayoutId = payPalPayoutService.createPayout(settings.getPaypalEmail(), payoutAmount,
				payout.getCurrency(), "Payout for author " + payout.getAuthor().getFullname());

		payout.setProviderPayoutId(providerPayoutId);
		payout.setProcessedDate(LocalDateTime.now());

		PayPalPayoutService.PayPalPayoutStatus providerStatus = payPalPayoutService
				.getPayoutBatchStatus(providerPayoutId);
		switch (providerStatus) {
			case SUCCESS:
				payout.setStatus(AuthorPayout.PayoutStatus.COMPLETED);
				payout.setCompletedDate(LocalDateTime.now());
				break;
			case FAILED:
				payout.setStatus(AuthorPayout.PayoutStatus.FAILED);
				payout.setFailureReason("PayPal reported payout failure for batch " + providerPayoutId);
				break;
			case PROCESSING:
			case PENDING:
				payout.setStatus(AuthorPayout.PayoutStatus.PROCESSING);
				break;
			case UNKNOWN:
			default:
				payout.setStatus(AuthorPayout.PayoutStatus.PENDING);
				break;
		}

		authorPayoutRepository.save(payout);
	}

	// === HELPER METHODS ===
	private AuthorPayoutSettings getOrCreatePayoutSettingsEntity(User author) {
		return authorPayoutSettingsRepository.findByAuthor(author).orElseGet(() -> {
			AuthorPayoutSettings settings = new AuthorPayoutSettings(author);
			settings.setMinimumPayoutAmount(defaultMinimumPayoutAmount);
			return authorPayoutSettingsRepository.save(settings);
		});
	}

	@Override
	public Page<AuthorPayoutDTO> listPayouts(AuthorPayout.PayoutStatus status, Pageable pageable) {
		Page<AuthorPayout> page = (status != null) ? authorPayoutRepository.findByStatus(status, pageable)
				: authorPayoutRepository.findAll(pageable);
		return new PageImpl<>(authorPayoutMapper.mapToDTOs(page.getContent()), pageable, page.getTotalElements());
	}

	@Override
	public AuthorPayoutDTO processPayout(UUID payoutId) throws Exception {
		AuthorPayout payout = authorPayoutRepository.findById(payoutId)
				.orElseThrow(() -> new IllegalArgumentException("Payout not found: " + payoutId));

		if (payout.getStatus() == AuthorPayout.PayoutStatus.PENDING) {
			processPayPalPayout(payout);
			payout = authorPayoutRepository.findById(payoutId).orElse(payout);
		}

		return authorPayoutMapper.mapToDTO(payout);
	}
}