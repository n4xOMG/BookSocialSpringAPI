package com.nix.dtos.mappers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.nix.dtos.CreditPackageDTO;
import com.nix.models.CreditPackage;

@Component
public class CreditPackageMapper implements Mapper<CreditPackage, CreditPackageDTO> {

	@Override
	public CreditPackageDTO mapToDTO(CreditPackage cpk) {
		if (cpk!=null) {
			CreditPackageDTO cpkDto = new CreditPackageDTO();
			if (cpk.getId()!=null) {
				cpkDto.setId(cpk.getId());
			}
			cpkDto.setActive(cpk.isActive());
			cpkDto.setCreditAmount(cpk.getCreditAmount());
			cpkDto.setName(cpk.getName());
			cpkDto.setPrice(cpk.getPrice());
			return cpkDto;
		}
		return null;
	}

	@Override
	public List<CreditPackageDTO> mapToDTOs(List<CreditPackage> cpks) {
		return cpks.stream().map(this::mapToDTO).collect(Collectors.toList());
	}

}
