package com.nix.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nix.models.Role;

public interface RoleRepository extends JpaRepository<Role, Integer> {
	Role findByName(String name);
}
