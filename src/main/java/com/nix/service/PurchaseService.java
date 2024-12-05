package com.nix.service;

import java.util.List;

import com.nix.dtos.SalesPerUserDTO;
import com.nix.models.Purchase;

public interface PurchaseService {

	List<Purchase> getPurchaseHistoryForUser(Integer userId);

	Double getTotalSalesAmount();

	Long getTotalNumberOfPurchases();

	List<SalesPerUserDTO> getSalesStatisticsPerUser();

}