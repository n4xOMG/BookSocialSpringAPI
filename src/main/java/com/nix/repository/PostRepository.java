package com.nix.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nix.models.Post;
import com.nix.models.User;

public interface PostRepository extends JpaRepository<Post, Long> {
	List<Post> findAllByOrderByTimestampDesc();

	List<Post> findByUser(User user);

}
