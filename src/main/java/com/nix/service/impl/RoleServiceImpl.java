package com.nix.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nix.models.Role;
import com.nix.models.User;
import com.nix.repository.RoleRepository;
import com.nix.repository.UserRepository;
import com.nix.service.RoleService;

@Service
public class RoleServiceImpl implements RoleService {

	@Autowired
	UserRepository userRepo;

	@Autowired
	RoleRepository roleRepo;

	@Override
	public Role createRole(Role role) {
		Role newRole = new Role();
		newRole.setName(role.getName());

		return roleRepo.save(newRole);
	}

	@Override
	public List<Role> getAllRoles() {
		return roleRepo.findAll();
	}

	@Override
	public Role updateRole(Role role, Integer roleId) throws Exception {
		Role updateRole = findRoleById(roleId);
		if (role.getName() != null) {
			updateRole.setName(role.getName());
		}
		return roleRepo.save(updateRole);
	}

	@Override
	public String deleteRole(Integer roleId) throws Exception {
		Role deleteRole = findRoleById(roleId);
		try {
			if (deleteRole != null) {
				for (User user : deleteRole.getUsers()) {
					user.setRole(null);
					userRepo.save(user);
				}
				roleRepo.delete(deleteRole);

			}
			return "Role deleted successfully!";
		} catch (Exception e) {
			return "Error deleting role" + e;
		}

	}

	@Override
	public Role addRoleToUser(Role role, User user) {
		user.setRole(role);
		role.getUsers().add(user);

		userRepo.save(user);
		return roleRepo.save(role);
	}

	@Override
	public Role addRoleToUsers(Role role, List<User> users) {
		for (User user : users) {
			user.setRole(role);

			role.getUsers().add(user);

			userRepo.save(user);
		}
		return roleRepo.save(role);

	}

	@Override
	public Role findRoleById(Integer roleId) throws Exception {
		Optional<Role> foundRole = roleRepo.findById(roleId);
		if (foundRole != null) {
			return foundRole.get();
		}
		throw new Exception("No role found with id: " + roleId);
	}

	@Override
	@Transactional
	public Role findRoleByName(String name) {
		Role role = roleRepo.findByName(name);
		return role;
	}

}
