package com.nix.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.nix.dtos.RoleDTO;
import com.nix.dtos.mappers.RoleMapper;
import com.nix.models.Role;
import com.nix.response.ApiResponse;
import com.nix.service.RoleService;
import com.nix.service.UserService;

@RestController
public class RoleController {

	@Autowired
	RoleService roleService;

	@Autowired
	UserService userService;

	RoleMapper roleMapper = new RoleMapper();

	@PostMapping("/admin/roles")
	public ResponseEntity<Role> addNewRole(@RequestBody Role role) throws Exception {
		Role newRole = roleService.createRole(role);
		return new ResponseEntity<>(newRole, HttpStatus.CREATED);

	}

	@PutMapping("/admin/roles/{roleId}")
	public ResponseEntity<RoleDTO> updateRole(@RequestBody Role role, @PathVariable("roleId") Integer roleId)
			throws Exception {
		RoleDTO updatedRole = roleMapper.mapToDTO(roleService.updateRole(role, roleId));

		return new ResponseEntity<>(updatedRole, HttpStatus.ACCEPTED);

	}

	@DeleteMapping("/admin/roles/{roleId}")
	public ResponseEntity<ApiResponse> deleteRole(@PathVariable("roleId") Integer roleId) throws Exception {
		ApiResponse res = new ApiResponse(roleService.deleteRole(roleId), true);

		return new ResponseEntity<>(res, HttpStatus.OK);
	}

	@GetMapping("/admin/roles")
	public ResponseEntity<List<RoleDTO>> getAllRoles() {
		List<RoleDTO> roles = roleMapper.mapToDTOs(roleService.getAllRoles());

		if (roles != null) {
			return new ResponseEntity<List<RoleDTO>>(roles, HttpStatus.OK);
		}
		return new ResponseEntity<List<RoleDTO>>(roles, HttpStatus.NO_CONTENT);
	}

}
