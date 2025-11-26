package com.nix.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.nix.response.ApiResponseWithData;
import com.nix.service.RoleService;

@RestController
@PreAuthorize("hasAnyRole('ADMIN')")
public class RoleController {

	private static final Logger logger = LoggerFactory.getLogger(RoleController.class);

	@Autowired
	RoleService roleService;

	RoleMapper roleMapper = new RoleMapper();

	@PostMapping("/admin/roles")
	public ResponseEntity<ApiResponseWithData<RoleDTO>> addNewRole(@RequestBody Role role) {
		try {
			Role newRole = roleService.createRole(role);
			RoleDTO dto = roleMapper.mapToDTO(newRole);
			return buildSuccessResponse(HttpStatus.CREATED, "Role created successfully.", dto);
		} catch (Exception e) {
			logger.error("Failed to create role", e);
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create role.");
		}
	}

	@PutMapping("/admin/roles/{roleId}")
	public ResponseEntity<ApiResponseWithData<RoleDTO>> updateRole(@RequestBody Role role,
			@PathVariable("roleId") Integer roleId) {
		try {
			RoleDTO updatedRole = roleMapper.mapToDTO(roleService.updateRole(role, roleId));
			return buildSuccessResponse(HttpStatus.ACCEPTED, "Role updated successfully.", updatedRole);
		} catch (Exception e) {
			logger.error("Failed to update role {}", roleId, e);
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update role.");
		}
	}

	@DeleteMapping("/admin/roles/{roleId}")
	public ResponseEntity<ApiResponseWithData<Void>> deleteRole(@PathVariable("roleId") Integer roleId) {
		try {
			roleService.deleteRole(roleId);
			return this.<Void>buildSuccessResponse("Role deleted successfully.", null);
		} catch (Exception e) {
			logger.error("Failed to delete role {}", roleId, e);
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete role.");
		}
	}

	@GetMapping("/admin/roles")
	public ResponseEntity<ApiResponseWithData<List<RoleDTO>>> getAllRoles() {
		try {
			List<RoleDTO> roles = roleMapper.mapToDTOs(roleService.getAllRoles());
			return buildSuccessResponse("Roles retrieved successfully.", roles);
		} catch (Exception e) {
			logger.error("Failed to retrieve roles", e);
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve roles.");
		}
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildSuccessResponse(String message, T data) {
		return ResponseEntity.ok(new ApiResponseWithData<>(message, true, data));
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildSuccessResponse(HttpStatus status, String message, T data) {
		return ResponseEntity.status(status).body(new ApiResponseWithData<>(message, true, data));
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildErrorResponse(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(new ApiResponseWithData<>(message, false, null));
	}
}
