package com.nix.dtos.mappers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.nix.dtos.AuthorPayoutDTO;
import com.nix.models.AuthorPayout;

@Component
public class AuthorPayoutMapper implements Mapper<AuthorPayout, AuthorPayoutDTO> {

	@Override
	public AuthorPayoutDTO mapToDTO(AuthorPayout payout) {
		AuthorPayoutDTO dto = new AuthorPayoutDTO();
		dto.setId(payout.getId());
		if (payout.getAuthor() != null) {
			dto.setAuthorId(payout.getAuthor().getId());
			dto.setAuthorName(payout.getAuthor().getUsername());
		}
		dto.setTotalAmount(payout.getTotalAmount());
		dto.setPlatformFeesDeducted(payout.getPlatformFeesDeducted());
		dto.setCurrency(payout.getCurrency());
		dto.setRequestedDate(payout.getRequestedDate());
		dto.setProcessedDate(payout.getProcessedDate());
		dto.setCompletedDate(payout.getCompletedDate());
		dto.setStatus(payout.getStatus());
		dto.setProviderPayoutId(payout.getProviderPayoutId());
		dto.setFailureReason(payout.getFailureReason());
		dto.setNotes(payout.getNotes());
		dto.setEarningsCount(payout.getEarnings() != null ? payout.getEarnings().size() : 0);
		return dto;
	}

	@Override
	public List<AuthorPayoutDTO> mapToDTOs(List<AuthorPayout> payouts) {
		return payouts.stream().map(this::mapToDTO).collect(Collectors.toList());
	}

}
