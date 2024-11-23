package com.nix.service;

import java.util.List;

import com.nix.models.CreditPackage;

public interface CreditPackageService {
	public List<CreditPackage> getAllCreditPackages();

	public List<CreditPackage> getActiveCreditPackages();

	public CreditPackage getCreditPackageById(Integer id);

	public CreditPackage createCreditPackage(CreditPackage creditPackage);

	public CreditPackage updateCreditPackage(Integer id, CreditPackage updatedPackage);

	public void deleteCreditPackage(Integer id);

	public List<CreditPackage> searchCreditPackagesByName(String name);

	public List<CreditPackage> getCreditPackagesByPrice(double price);

	public List<CreditPackage> getCreditPackagesSortedByCreditAmountDesc();

	public CreditPackage activateCreditPackage(Integer id);

	public CreditPackage deactivateCreditPackage(Integer id);
}
