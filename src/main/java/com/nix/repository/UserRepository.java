package com.nix.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.nix.models.User;

public interface UserRepository extends JpaRepository<User, Integer>{
	public User findByEmail(String email);
	
	
	@Query("SELECT u FROM User u WHERE u.verificationCode = :code")
    public User findByVerificationCode(String code);
	
	Page<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(String username, String email, Pageable pageable);
}
