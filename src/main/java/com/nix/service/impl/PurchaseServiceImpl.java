package com.nix.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nix.dtos.PurchaseDTO;
import com.nix.dtos.SalesPerUserDTO;
import com.nix.dtos.mappers.PurchaseMapper;
import com.nix.models.Purchase;
import com.nix.models.User;
import com.nix.repository.PurchaseRepository;
import com.nix.repository.UserRepository;
import com.nix.service.PurchaseService;

import jakarta.persistence.EntityNotFoundException;

@Service
public class PurchaseServiceImpl implements PurchaseService {

	@Autowired
	private PurchaseRepository purchaseRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PurchaseMapper purchaseMapper;

	@Override
	public List<PurchaseDTO> getPurchaseHistoryForUser(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

		List<Purchase> purchases = purchaseRepository.findByUser(user);
		return purchaseMapper.mapToDTOs(purchases);
	}

	@Override
	public Double getTotalSalesAmount() {
		BigDecimal total = purchaseRepository.findAll().stream().map(Purchase::getAmount)
				.filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
		return total.doubleValue();
	}

	@Override
	public Long getTotalNumberOfPurchases() {
		return purchaseRepository.count();
	}

	@Override
	public List<SalesPerUserDTO> getSalesStatisticsPerUser() {
		return purchaseRepository.findAll().stream().map(Purchase::getUser).distinct().map(user -> {
			BigDecimal totalSpent = purchaseRepository.findByUser(user).stream().map(Purchase::getAmount)
					.filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
			return new SalesPerUserDTO(user.getId(), user.getUsername(), totalSpent.doubleValue());
		}).collect(Collectors.toList());
	}
}
