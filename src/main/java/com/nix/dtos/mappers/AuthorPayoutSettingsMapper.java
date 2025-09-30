package com.nix.dtos.mappers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.nix.dtos.AuthorPayoutSettingsDTO;
import com.nix.models.AuthorPayoutSettings;

@Component
public class AuthorPayoutSettingsMapper implements Mapper<AuthorPayoutSettings, AuthorPayoutSettingsDTO> {

	@Override
	public AuthorPayoutSettingsDTO mapToDTO(AuthorPayoutSettings settings) {
		AuthorPayoutSettingsDTO dto = new AuthorPayoutSettingsDTO();
		dto.setId(settings.getId());
		dto.setAuthorId(settings.getAuthor() != null ? settings.getAuthor().getId() : null);
		dto.setMinimumPayoutAmount(settings.getMinimumPayoutAmount());
		dto.setPayoutFrequency(settings.getPayoutFrequency());
		dto.setPaypalEmail(settings.getPaypalEmail());
		dto.setAutoPayoutEnabled(settings.isAutoPayoutEnabled());
		dto.setLastPayoutDate(settings.getLastPayoutDate());
		dto.setCreatedDate(settings.getCreatedDate());
		dto.setUpdatedDate(settings.getUpdatedDate());
		dto.setPaymentMethodType(settings.getPaymentMethodType());
		dto.setAccountHolderName(settings.getAccountHolderName());
		dto.setBankName(settings.getBankName());
		dto.setAccountLastFour(settings.getAccountLastFour());
		return dto;
	}

	@Override
	public List<AuthorPayoutSettingsDTO> mapToDTOs(List<AuthorPayoutSettings> entities) {
		return entities.stream().map(this::mapToDTO).collect(Collectors.toList());
	}
}
