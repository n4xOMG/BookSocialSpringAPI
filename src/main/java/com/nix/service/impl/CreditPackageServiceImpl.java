package com.nix.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nix.exception.ResourceNotFoundException;
import com.nix.models.CreditPackage;
import com.nix.repository.CreditPackageRepository;
import com.nix.repository.PurchaseRepository;
import com.nix.service.CreditPackageService;
import com.nix.enums.PaymentStatus;

@Service
public class CreditPackageServiceImpl implements CreditPackageService {

	private static final int EXCHANGE_RATE_SCALE = 4;

	@Autowired
	CreditPackageRepository creditPackageRepository;

	@Autowired
	PurchaseRepository purchaseRepository;

	@Value("${app.credit.default-usd-per-credit:0.01}")
	private BigDecimal defaultUsdPricePerCredit;

	@Override
	public List<CreditPackage> getAllCreditPackages() {
		return creditPackageRepository.findAll();
	}

	@Override
	public List<CreditPackage> getActiveCreditPackages() {
		return creditPackageRepository.findByIsActiveTrue();
	}

	@Override
	public CreditPackage getCreditPackageById(Long id) {
		return creditPackageRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("CreditPackage not found with ID: " + id));
	}

	@Override
	public CreditPackage createCreditPackage(CreditPackage creditPackage) {
		validateCreditPackageValues(creditPackage.getCreditAmount(), creditPackage.getPrice());
		if (creditPackageRepository.existsByName(creditPackage.getName())) {
			throw new IllegalArgumentException(
					"CreditPackage with name '" + creditPackage.getName() + "' already exists.");
		}
		creditPackage.setActive(true);
		return creditPackageRepository.save(creditPackage);
	}

	@Override
	public CreditPackage updateCreditPackage(Long id, CreditPackage updatedPackage) {
		CreditPackage existingPackage = getCreditPackageById(id);
		validateCreditPackageValues(updatedPackage.getCreditAmount(), updatedPackage.getPrice());
		// Update fields
		existingPackage.setName(updatedPackage.getName());
		existingPackage.setCreditAmount(updatedPackage.getCreditAmount());
		existingPackage.setPrice(updatedPackage.getPrice());
		existingPackage.setActive(updatedPackage.isActive());

		return creditPackageRepository.save(existingPackage);
	}

	@Override
	public void deleteCreditPackage(Long id) {
		CreditPackage existingPackage = creditPackageRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("CreditPackage not found with ID: " + id));
		creditPackageRepository.delete(existingPackage);

	}

	@Override
	public List<CreditPackage> searchCreditPackagesByName(String name) {
		return creditPackageRepository.findByNameContainingIgnoreCase(name);
	}

	@Override
	public List<CreditPackage> getCreditPackagesByPrice(double price) {
		return creditPackageRepository.findByPriceLessThanEqual(price);
	}

	@Override
	public List<CreditPackage> getCreditPackagesSortedByCreditAmountDesc() {
		return creditPackageRepository.findAllByOrderByCreditAmountDesc();
	}

	@Override
	public CreditPackage activateCreditPackage(Long id) {
		CreditPackage creditPackage = getCreditPackageById(id);
		creditPackage.setActive(true);
		return creditPackageRepository.save(creditPackage);
	}

	@Override
	public CreditPackage deactivateCreditPackage(Long id) {
		CreditPackage creditPackage = getCreditPackageById(id);
		creditPackage.setActive(false);
		return creditPackageRepository.save(creditPackage);
	}

	@Override
	public BigDecimal calculateUsdPricePerCredit(Long creditPackageId) {
		CreditPackage creditPackage = getCreditPackageById(creditPackageId);
		return creditPackage.calculatePricePerCredit();
	}

	@Override
	public BigDecimal getCurrentUsdPricePerCredit() {
		BigDecimal fromPurchases = derivePriceFromCompletedPurchases();
		if (fromPurchases.compareTo(BigDecimal.ZERO) > 0) {
			return fromPurchases;
		}
		BigDecimal fromPackages = derivePriceFromActivePackages();
		if (fromPackages.compareTo(BigDecimal.ZERO) > 0) {
			return fromPackages;
		}
		return resolveDefaultUsdPricePerCredit();
	}

	private BigDecimal derivePriceFromCompletedPurchases() {
		Object[] totals = purchaseRepository.getTotalsByStatus(PaymentStatus.COMPLETED);
		if (totals == null || totals.length < 2) {
			return BigDecimal.ZERO;
		}
		BigDecimal totalAmount = extractBigDecimal(totals[0]);
		long totalCredits = extractLong(totals[1]);
		if (totalCredits <= 0 || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
			return BigDecimal.ZERO;
		}
		return totalAmount.divide(BigDecimal.valueOf(totalCredits), EXCHANGE_RATE_SCALE, RoundingMode.HALF_UP);
	}

	private BigDecimal derivePriceFromActivePackages() {
		List<CreditPackage> activePackages = getActiveCreditPackages();
		BigDecimal totalPrice = BigDecimal.ZERO;
		int totalCredits = 0;
		for (CreditPackage creditPackage : activePackages) {
			if (creditPackage.getPrice() <= 0 || creditPackage.getCreditAmount() <= 0) {
				continue;
			}
			totalPrice = totalPrice.add(BigDecimal.valueOf(creditPackage.getPrice()));
			totalCredits += creditPackage.getCreditAmount();
		}
		if (totalCredits <= 0 || totalPrice.compareTo(BigDecimal.ZERO) <= 0) {
			return BigDecimal.ZERO;
		}
		return totalPrice.divide(BigDecimal.valueOf(totalCredits), EXCHANGE_RATE_SCALE, RoundingMode.HALF_UP);
	}

	private BigDecimal extractBigDecimal(Object value) {
		if (value instanceof BigDecimal) {
			return (BigDecimal) value;
		}
		if (value instanceof Number) {
			return BigDecimal.valueOf(((Number) value).doubleValue());
		}
		return BigDecimal.ZERO;
	}

	private long extractLong(Object value) {
		if (value instanceof Number) {
			return ((Number) value).longValue();
		}
		return 0L;
	}

	private BigDecimal resolveDefaultUsdPricePerCredit() {
		if (defaultUsdPricePerCredit == null || defaultUsdPricePerCredit.compareTo(BigDecimal.ZERO) <= 0) {
			return BigDecimal.valueOf(0.01);
		}
		return defaultUsdPricePerCredit;
	}

	private void validateCreditPackageValues(int creditAmount, double price) {
		if (creditAmount <= 0) {
			throw new IllegalArgumentException("Credit amount must be greater than zero.");
		}
		if (price <= 0) {
			throw new IllegalArgumentException("Price must be greater than zero.");
		}
	}

}
