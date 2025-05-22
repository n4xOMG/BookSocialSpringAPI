package com.nix.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nix.models.User;
import com.nix.models.UserFollow;

public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {
	Optional<UserFollow> findByFollowerAndFollowed(User follower, User followed);

	List<UserFollow> findByFollowedId(Long userId);

	List<UserFollow> findByFollowerId(Long userId);
}
