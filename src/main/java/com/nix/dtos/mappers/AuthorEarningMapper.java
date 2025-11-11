package com.nix.dtos.mappers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.nix.dtos.AuthorEarningDTO;
import com.nix.models.AuthorEarning;

@Component
public class AuthorEarningMapper implements Mapper<AuthorEarning, AuthorEarningDTO> {

	@Override
	public AuthorEarningDTO mapToDTO(AuthorEarning earning) {
		AuthorEarningDTO dto = new AuthorEarningDTO();
		dto.setId(earning.getId());
		if (earning.getChapter() != null) {
			dto.setChapterId(earning.getChapter().getId());
			dto.setChapterTitle(earning.getChapter().getTitle());
			dto.setChapterNumber(earning.getChapter().getChapterNum());
			if (earning.getChapter().getBook() != null) {
				dto.setBookId(earning.getChapter().getBook().getId());
				dto.setBookTitle(earning.getChapter().getBook().getTitle());
			}
		}
		dto.setGrossAmount(earning.getGrossAmount());
		dto.setPlatformFee(earning.getPlatformFee());
		dto.setNetAmount(earning.getNetAmount());
		dto.setPlatformFeePercentage(earning.getPlatformFeePercentage());
		dto.setCurrency(earning.getCurrency());
		dto.setEarnedDate(earning.getEarnedDate());
		dto.setPaidOut(earning.isPaidOut());
		dto.setPayoutId(earning.getPayout() != null ? earning.getPayout().getId() : null);
		return dto;
	}

	@Override
	public List<AuthorEarningDTO> mapToDTOs(List<AuthorEarning> entities) {
		return entities.stream().map(this::mapToDTO).collect(Collectors.toList());
	}

}
