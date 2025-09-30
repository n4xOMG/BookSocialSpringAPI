package com.nix.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
import com.nix.models.AuthorEarning;
import com.nix.models.AuthorPayout;
import com.nix.models.AuthorPayoutSettings;
import com.nix.models.Book;
import com.nix.models.Chapter;
import com.nix.models.ChapterUnlockRecord;
import com.nix.models.User;
import com.nix.repository.AuthorEarningRepository;
import com.nix.repository.AuthorPayoutRepository;
import com.nix.repository.AuthorPayoutSettingsRepository;
import com.nix.repository.BookRepository;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.Transfer;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.param.TransferCreateParams;

@Service
@Transactional
public class AuthorServiceImpl implements AuthorService {

	@Value("${app.platform.fee.percentage:7.5}")
	private BigDecimal platformFeePercentage;

	@Value("${stripe.apiKey}")
	private String stripeApiKey;

	@Value("${frontend.url}")
	private String frontendUrl;

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

	// === EARNINGS METHODS ===

	@Override
	public AuthorEarning recordChapterUnlockEarning(ChapterUnlockRecord unlockRecord) {
		Chapter chapter = unlockRecord.getChapter();
		User author = chapter.getBook().getAuthor();

		BigDecimal grossAmount = BigDecimal.valueOf(unlockRecord.getUnlockCost());

		AuthorEarning earning = new AuthorEarning(author, chapter, unlockRecord, grossAmount, platformFeePercentage);

		return authorEarningRepository.save(earning);
	}

	@Override
	public AuthorDashboardDTO getAuthorDashboard(User author, Pageable pageable) {
		AuthorDashboardDTO dashboard = new AuthorDashboardDTO();

		// Earnings summary
		dashboard.setTotalLifetimeEarnings(getLifetimeEarnings(author));
		dashboard.setTotalUnpaidEarnings(getUnpaidEarnings(author));
		dashboard.setTotalPaidOut(authorPayoutRepository.getTotalPaidOutForAuthor(author));
		dashboard.setCurrentBalance(dashboard.getTotalUnpaidEarnings());

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

		// Recent activity
		dashboard.setRecentEarnings(getAuthorEarnings(author, Pageable.ofSize(5)).getContent());

		List<AuthorPayout> recentPayouts = authorPayoutRepository
				.findByAuthorOrderByRequestedDateDesc(author, Pageable.ofSize(5)).getContent();
		dashboard.setRecentPayouts(recentPayouts.stream().map(this::convertToPayoutDTO).collect(Collectors.toList()));

		// Payout settings
		AuthorPayoutSettings settings = getPayoutSettings(author);
		dashboard.setMinimumPayoutAmount(settings.getMinimumPayoutAmount());
		dashboard.setStripeConnected(settings.isStripeAccountVerified());
		dashboard.setCanRequestPayout(canRequestPayout(author));

		return dashboard;
	}

	@Override
	public Page<AuthorEarningDTO> getAuthorEarnings(User author, Pageable pageable) {
		Page<AuthorEarning> earnings = authorEarningRepository.findByAuthorOrderByEarnedDateDesc(author, pageable);

		List<AuthorEarningDTO> dtos = earnings.getContent().stream().map(this::convertToEarningDTO)
				.collect(Collectors.toList());

		return new PageImpl<>(dtos, pageable, earnings.getTotalElements());
	}

	@Override
	public BigDecimal getUnpaidEarnings(User author) {
		return authorEarningRepository.getTotalUnpaidEarningsForAuthor(author);
	}

	@Override
	public BigDecimal getLifetimeEarnings(User author) {
		return authorEarningRepository.getTotalLifetimeEarningsForAuthor(author);
	}

	// === PAYOUT METHODS ===

	@Override
	public AuthorPayout requestPayout(User author, BigDecimal amount) throws Exception {
		if (!canRequestPayout(author)) {
			throw new Exception("Payout requirements not met");
		}

		BigDecimal availableBalance = getUnpaidEarnings(author);
		if (amount.compareTo(availableBalance) > 0) {
			throw new Exception("Requested amount exceeds available balance");
		}

		AuthorPayoutSettings settings = getPayoutSettings(author);
		if (amount.compareTo(settings.getMinimumPayoutAmount()) < 0) {
			throw new Exception("Amount below minimum payout threshold");
		}

		// Get unpaid earnings
		List<AuthorEarning> unpaidEarnings = authorEarningRepository
				.findByAuthorAndIsPaidOutFalseOrderByEarnedDateDesc(author);

		BigDecimal totalPlatformFees = unpaidEarnings.stream().map(AuthorEarning::getPlatformFee)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		AuthorPayout payout = new AuthorPayout(author, amount, totalPlatformFees);
		AuthorPayout savedPayout = authorPayoutRepository.save(payout);

		// Mark earnings as paid out
		unpaidEarnings.forEach(earning -> {
			earning.setPaidOut(true);
			earning.setPayout(savedPayout);
		});
		authorEarningRepository.saveAll(unpaidEarnings);

		return savedPayout;
	}

	@Override
	public Page<AuthorPayoutDTO> getAuthorPayouts(User author, Pageable pageable) {
		Page<AuthorPayout> payouts = authorPayoutRepository.findByAuthorOrderByRequestedDateDesc(author, pageable);

		List<AuthorPayoutDTO> dtos = payouts.getContent().stream().map(this::convertToPayoutDTO)
				.collect(Collectors.toList());

		return new PageImpl<>(dtos, pageable, payouts.getTotalElements());
	}

	@Override
	public AuthorPayoutSettings getPayoutSettings(User author) {
		return authorPayoutSettingsRepository.findByAuthor(author).orElseGet(() -> {
			AuthorPayoutSettings settings = new AuthorPayoutSettings(author);
			settings.setMinimumPayoutAmount(defaultMinimumPayoutAmount);
			return authorPayoutSettingsRepository.save(settings);
		});
	}

	@Override
	public AuthorPayoutSettings updatePayoutSettings(User author, AuthorPayoutSettings settings) {
		AuthorPayoutSettings existing = getPayoutSettings(author);

		existing.setMinimumPayoutAmount(settings.getMinimumPayoutAmount());
		existing.setPayoutFrequency(settings.getPayoutFrequency());
		existing.setAutoPayoutEnabled(settings.isAutoPayoutEnabled());
		existing.setUpdatedDate(LocalDateTime.now());

		return authorPayoutSettingsRepository.save(existing);
	}

	@Override
	public String createStripeConnectAccountLink(User author) throws Exception {
		com.stripe.Stripe.apiKey = stripeApiKey;

		AuthorPayoutSettings settings = getPayoutSettings(author);

		Account account;
		if (settings.getStripeAccountId() == null) {
			AccountCreateParams accountParams = AccountCreateParams.builder().setType(AccountCreateParams.Type.EXPRESS)
					.setCountry("US").setEmail(author.getEmail()).putMetadata("author_id", author.getId().toString())
					.putMetadata("author_name", author.getFullname()).build();

			account = Account.create(accountParams);

			settings.setStripeAccountId(account.getId());
			authorPayoutSettingsRepository.save(settings);
		} else {
			account = Account.retrieve(settings.getStripeAccountId());
		}

		AccountLinkCreateParams linkParams = AccountLinkCreateParams.builder().setAccount(account.getId())
				.setRefreshUrl(frontendUrl + "/author/payout-settings?refresh=true")
				.setReturnUrl(frontendUrl + "/author/payout-settings?success=true")
				.setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING).build();

		AccountLink accountLink = AccountLink.create(linkParams);
		return accountLink.getUrl();
	}

	@Override
	public boolean canRequestPayout(User author) {
		AuthorPayoutSettings settings = getPayoutSettings(author);
		BigDecimal availableBalance = getUnpaidEarnings(author);

		return settings.isStripeAccountVerified() && availableBalance.compareTo(settings.getMinimumPayoutAmount()) >= 0;
	}

	@Override
	public BigDecimal getPlatformFeePercentage() {
		return platformFeePercentage;
	}

	// === STRIPE PROCESSING (called by scheduler) ===

	public void processStripeTransfer(AuthorPayout payout) throws Exception {
		com.stripe.Stripe.apiKey = stripeApiKey;

		AuthorPayoutSettings settings = authorPayoutSettingsRepository.findByAuthor(payout.getAuthor())
				.orElseThrow(() -> new Exception("Payout settings not found"));

		if (!settings.isStripeAccountVerified()) {
			throw new Exception("Stripe account not verified");
		}

		long amountInCents = payout.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue();

		TransferCreateParams transferParams = TransferCreateParams.builder().setAmount(amountInCents).setCurrency("usd")
				.setDestination(settings.getStripeAccountId()).putMetadata("payout_id", payout.getId().toString())
				.putMetadata("author_id", payout.getAuthor().getId().toString()).build();

		Transfer transfer = Transfer.create(transferParams);

		payout.setStatus(AuthorPayout.PayoutStatus.PROCESSING);
		payout.setProcessedDate(LocalDateTime.now());
		payout.setStripePayoutId(transfer.getId());
		authorPayoutRepository.save(payout);
	}

	public void handleStripePayoutWebhook(String payoutId, String status, String failureReason) {
		AuthorPayout payout = authorPayoutRepository.findByStripePayoutId(payoutId);
		if (payout != null) {
			switch (status) {
			case "paid":
				payout.setStatus(AuthorPayout.PayoutStatus.COMPLETED);
				payout.setCompletedDate(LocalDateTime.now());
				break;
			case "failed":
				payout.setStatus(AuthorPayout.PayoutStatus.FAILED);
				payout.setFailureReason(failureReason);
				break;
			}
			authorPayoutRepository.save(payout);
		}
	}

	// === HELPER METHODS ===

	private AuthorEarningDTO convertToEarningDTO(AuthorEarning earning) {
		AuthorEarningDTO dto = new AuthorEarningDTO();
		dto.setId(earning.getId());
		dto.setChapterId(earning.getChapter().getId());
		dto.setChapterTitle(earning.getChapter().getTitle());
		dto.setChapterNumber(earning.getChapter().getChapterNum());
		dto.setBookId(earning.getChapter().getBook().getId());
		dto.setBookTitle(earning.getChapter().getBook().getTitle());
		dto.setGrossAmount(earning.getGrossAmount());
		dto.setPlatformFee(earning.getPlatformFee());
		dto.setNetAmount(earning.getNetAmount());
		dto.setPlatformFeePercentage(earning.getPlatformFeePercentage());
		dto.setEarnedDate(earning.getEarnedDate());
		dto.setPaidOut(earning.isPaidOut());
		if (earning.getPayout() != null) {
			dto.setPayoutId(earning.getPayout().getId());
		}
		return dto;
	}

	private AuthorPayoutDTO convertToPayoutDTO(AuthorPayout payout) {
		AuthorPayoutDTO dto = new AuthorPayoutDTO();
		dto.setId(payout.getId());
		dto.setAuthorId(payout.getAuthor().getId());
		dto.setAuthorName(payout.getAuthor().getFullname());
		dto.setTotalAmount(payout.getTotalAmount());
		dto.setPlatformFeesDeducted(payout.getPlatformFeesDeducted());
		dto.setRequestedDate(payout.getRequestedDate());
		dto.setProcessedDate(payout.getProcessedDate());
		dto.setCompletedDate(payout.getCompletedDate());
		dto.setStatus(payout.getStatus());
		dto.setStripePayoutId(payout.getStripePayoutId());
		dto.setFailureReason(payout.getFailureReason());
		dto.setNotes(payout.getNotes());
		dto.setEarningsCount(payout.getEarnings().size());
		return dto;
	}
}