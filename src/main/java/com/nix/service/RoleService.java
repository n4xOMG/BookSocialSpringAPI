package com.nix.service;

import java.util.List;

import com.nix.models.Role;
import com.nix.models.User;

public interface RoleService {
	public Role findRoleById(Integer roleId) throws Exception;

	public Role findRoleByName(String name);

	public Role createRole(Role role);

	public List<Role> getAllRoles();

	public Role updateRole(Role role, Integer roleId) throws Exception;

	public String deleteRole(Integer roleId) throws Exception;

	public Role addRoleToUser(Role role, User user);

	public Role addRoleToUsers(Role role, List<User> user);

}
