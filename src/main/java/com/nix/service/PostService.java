package com.nix.service;

import java.util.List;

import com.nix.models.Post;
import com.nix.models.User;

public interface PostService {
	List<Post> getAllPosts();
	
	List<Post> getPostsByUser(User user);

	Post createPost(User user, Post post);

	Post updatePost(User user, Integer postId, Post postDetails);

	void deletePost(User user, Integer postId);

	Post likePost(Integer postId);
}
