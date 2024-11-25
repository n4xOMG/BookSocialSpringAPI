package com.nix.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nix.models.Chat;
import com.nix.models.User;

public interface ChatRepository extends JpaRepository<Chat, Long> {
	Optional<Chat> findByUserOneAndUserTwo(User userOne, User userTwo);

	List<Chat> findByUserOneOrUserTwo(User userOne, User userTwo);
}
