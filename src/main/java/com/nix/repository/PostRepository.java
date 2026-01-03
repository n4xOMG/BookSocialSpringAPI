package com.nix.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nix.models.Post;
import com.nix.models.User;

public interface PostRepository extends JpaRepository<Post, UUID> {
	List<Post> findAllByOrderByTimestampDesc();

	List<Post> findByUser(User user);

	Page<Post> findByUser_IdNotIn(List<UUID> userIds, Pageable pageable);

	List<Post> findBySharedPost(Post sharedPost);

}
