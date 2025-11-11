package com.nix.dtos.mappers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.nix.dtos.PurchaseDTO;
import com.nix.models.Purchase;

@Component
public class PurchaseMapper implements Mapper<Purchase, PurchaseDTO> {
	CreditPackageMapper creditPackageMapper = new CreditPackageMapper();

	@Override
	public PurchaseDTO mapToDTO(Purchase p) {
		if (p != null) {
			PurchaseDTO purchaseDTO = new PurchaseDTO();
			if (p.getId() != null) {
				purchaseDTO.setId(p.getId());
			}
			purchaseDTO.setCreditPackage(creditPackageMapper.mapToDTO(p.getCreditPackage()));
			purchaseDTO.setAmount(p.getAmount());
			purchaseDTO.setPaymentIntentId(p.getPaymentIntentId());
			purchaseDTO.setPurchaseDate(p.getPurchaseDate());
			purchaseDTO.setPaymentProvider(p.getPaymentProvider());
			purchaseDTO.setStatus(p.getStatus());
			purchaseDTO.setCurrency(p.getCurrency());
			purchaseDTO.setCreditsPurchased(p.getCreditsPurchased());
			return purchaseDTO;
		}
		return null;
	}

	@Override
	public List<PurchaseDTO> mapToDTOs(List<Purchase> purchases) {
		return purchases.stream().map(this::mapToDTO).collect(Collectors.toList());
	}
}
