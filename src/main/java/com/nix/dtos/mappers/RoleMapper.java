package com.nix.dtos.mappers;

import java.util.List;
import java.util.stream.Collectors;

import com.nix.dtos.RoleDTO;
import com.nix.models.Role;

public class RoleMapper implements Mapper<Role, RoleDTO> {

	@Override
	public RoleDTO mapToDTO(Role role) {
		RoleDTO roleDTO = new RoleDTO();

		if (role !=null && role.getId()!=null) {
			roleDTO.setId(role.getId());
		}
		roleDTO.setName(role.getName());

		return roleDTO;
	}

	@Override
	public List<RoleDTO> mapToDTOs(List<Role> roles) {
		return roles.stream().map(this::mapToDTO).collect(Collectors.toList());
	}

}
