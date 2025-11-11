package com.nix.service;

import java.math.BigDecimal;
import java.util.List;

import com.nix.models.CreditPackage;

public interface CreditPackageService {
	public List<CreditPackage> getAllCreditPackages();

	public List<CreditPackage> getActiveCreditPackages();

	public CreditPackage getCreditPackageById(Long id);

	public CreditPackage createCreditPackage(CreditPackage creditPackage);

	public CreditPackage updateCreditPackage(Long id, CreditPackage updatedPackage);

	public void deleteCreditPackage(Long id);

	public List<CreditPackage> searchCreditPackagesByName(String name);

	public List<CreditPackage> getCreditPackagesByPrice(double price);

	public List<CreditPackage> getCreditPackagesSortedByCreditAmountDesc();

	public CreditPackage activateCreditPackage(Long id);

	public CreditPackage deactivateCreditPackage(Long id);

	BigDecimal calculateUsdPricePerCredit(Long creditPackageId);

	BigDecimal getCurrentUsdPricePerCredit();

	default BigDecimal calculateUsdValueForCredits(int credits) {
		if (credits <= 0) {
			return BigDecimal.ZERO;
		}
		return getCurrentUsdPricePerCredit().multiply(BigDecimal.valueOf(credits));
	}

	default BigDecimal calculateUsdPricePerCredit(CreditPackage creditPackage) {
		if (creditPackage == null) {
			return BigDecimal.ZERO;
		}
		return creditPackage.calculatePricePerCredit();
	}
}
