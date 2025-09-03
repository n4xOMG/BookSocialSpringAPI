package com.nix.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nix.models.User;
import com.nix.models.UserFollow;

public interface UserFollowRepository extends JpaRepository<UserFollow, UUID> {
	Optional<UserFollow> findByFollowerAndFollowed(User follower, User followed);

	List<UserFollow> findByFollowedId(UUID userId);

	List<UserFollow> findByFollowerId(UUID userId);
}
