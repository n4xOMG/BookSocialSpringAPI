package com.nix.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nix.exception.ResourceNotFoundException;
import com.nix.models.CreditPackage;
import com.nix.repository.CreditPackageRepository;

@Service
public class CreditPackageServiceImpl implements CreditPackageService {

	@Autowired
	CreditPackageRepository creditPackageRepository;

	@Override
	public List<CreditPackage> getAllCreditPackages() {
		return creditPackageRepository.findAll();
	}

	@Override
	public List<CreditPackage> getActiveCreditPackages() {
		return creditPackageRepository.findByIsActiveTrue();
	}

	@Override
	public CreditPackage getCreditPackageById(Integer id) {
		return creditPackageRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("CreditPackage not found with ID: " + id));
	}

	@Override
	public CreditPackage createCreditPackage(CreditPackage creditPackage) {
		if (creditPackageRepository.existsByName(creditPackage.getName())) {
			throw new IllegalArgumentException(
					"CreditPackage with name '" + creditPackage.getName() + "' already exists.");
		}
		creditPackage.setActive(true);
		return creditPackageRepository.save(creditPackage);
	}

	@Override
	public CreditPackage updateCreditPackage(Integer id, CreditPackage updatedPackage) {
		CreditPackage existingPackage = getCreditPackageById(id);

		// Update fields
		existingPackage.setName(updatedPackage.getName());
		existingPackage.setCreditAmount(updatedPackage.getCreditAmount());
		existingPackage.setPrice(updatedPackage.getPrice());
		existingPackage.setActive(updatedPackage.isActive());

		return creditPackageRepository.save(existingPackage);
	}

	@Override
	public void deleteCreditPackage(Integer id) {
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
	public CreditPackage activateCreditPackage(Integer id) {
		CreditPackage creditPackage = getCreditPackageById(id);
		creditPackage.setActive(true);
		return creditPackageRepository.save(creditPackage);
	}

	@Override
	public CreditPackage deactivateCreditPackage(Integer id) {
		CreditPackage creditPackage = getCreditPackageById(id);
		creditPackage.setActive(false);
		return creditPackageRepository.save(creditPackage);
	}

}
