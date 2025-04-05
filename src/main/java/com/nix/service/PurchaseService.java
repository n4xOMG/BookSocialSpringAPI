package com.nix.service;

import java.util.List;

import com.nix.dtos.PurchaseDTO;
import com.nix.dtos.SalesPerUserDTO;

public interface PurchaseService {

	List<PurchaseDTO> getPurchaseHistoryForUser(Integer userId);

	Double getTotalSalesAmount();

	Long getTotalNumberOfPurchases();

	List<SalesPerUserDTO> getSalesStatisticsPerUser();

}