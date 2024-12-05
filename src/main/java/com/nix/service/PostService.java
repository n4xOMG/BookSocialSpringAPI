package com.nix.service;

import java.util.List;

import com.nix.dtos.PostDTO;
import com.nix.models.Post;
import com.nix.models.User;

public interface PostService {
	List<Post> getAllPosts();

	Post getPostById(Integer postId);

	List<Post> getPostsByUser(User user);

	Post createPost(User user, PostDTO post);

	Post updatePost(User user, Integer postId, Post postDetails);

	void deletePost(User user, Integer postId);

	Post likePost(Integer postId, User user);

	
}
