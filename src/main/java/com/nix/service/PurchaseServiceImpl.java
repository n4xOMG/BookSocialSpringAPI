package com.nix.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nix.dtos.SalesPerUserDTO;
import com.nix.models.Purchase;
import com.nix.models.User;
import com.nix.repository.PurchaseRepository;
import com.nix.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class PurchaseServiceImpl implements PurchaseService {

	@Autowired
	private PurchaseRepository purchaseRepository;
	@Autowired
	private UserRepository userRepository;

	@Override
	public List<Purchase> getPurchaseHistoryForUser(Integer userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

		List<Purchase> purchases = purchaseRepository.findByUser(user);

		return purchases;
	}

	@Override
	public Double getTotalSalesAmount() {
		return purchaseRepository.findAll().stream().mapToDouble(Purchase::getAmount).sum();
	}

	@Override
	public Long getTotalNumberOfPurchases() {
		return purchaseRepository.count();
	}

	@Override
	public List<SalesPerUserDTO> getSalesStatisticsPerUser() {
		return purchaseRepository.findAll().stream().map(Purchase::getUser).distinct().map(user -> {
			double totalSpent = purchaseRepository.findByUser(user).stream().mapToDouble(Purchase::getAmount).sum();
			return new SalesPerUserDTO(user.getId(), user.getUsername(), totalSpent);
		}).collect(Collectors.toList());
	}

}
