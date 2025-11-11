package com.nix.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nix.models.User;
import com.nix.models.UserBlock;

public interface UserBlockRepository extends JpaRepository<UserBlock, UUID> {

	boolean existsByBlockerAndBlocked(User blocker, User blocked);

	boolean existsByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

	Optional<UserBlock> findByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

	List<UserBlock> findByBlocker(User blocker);

	List<UserBlock> findByBlockerId(UUID blockerId);

	List<UserBlock> findByBlockedId(UUID blockedId);

	@Query("SELECT ub.blocker.id FROM UserBlock ub WHERE ub.blocked.id = :blockedUserId")
	Set<UUID> findBlockerIdsByBlockedId(@Param("blockedUserId") UUID blockedUserId);

	@Query("SELECT ub.blocked.id FROM UserBlock ub WHERE ub.blocker.id = :blockerId")
	Set<UUID> findBlockedIdsByBlockerId(@Param("blockerId") UUID blockerId);

}
