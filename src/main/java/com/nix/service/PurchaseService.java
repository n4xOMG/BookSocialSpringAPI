package com.nix.service;

import java.util.List;
import java.util.UUID;

import com.nix.dtos.PurchaseDTO;
import com.nix.dtos.SalesPerUserDTO;

public interface PurchaseService {

	List<PurchaseDTO> getPurchaseHistoryForUser(UUID userId);

	Double getTotalSalesAmount();

	Long getTotalNumberOfPurchases();

	List<SalesPerUserDTO> getSalesStatisticsPerUser();

}