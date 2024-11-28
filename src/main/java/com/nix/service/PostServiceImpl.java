package com.nix.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nix.exception.ResourceNotFoundException;
import com.nix.models.Post;
import com.nix.models.User;
import com.nix.repository.PostRepository;
import com.nix.repository.UserRepository;

@Service
public class PostServiceImpl implements PostService {

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private UserRepository userRepository;

	@Override
	public List<Post> getAllPosts() {
		return postRepository.findAllByOrderByTimestampDesc();
	}

	@Override
	public List<Post> getPostsByUser(User user) {
		return postRepository.findByUser(user);
	}

	@Override
	public Post createPost(User user, Post post) {
		post.setUser(user);
		post.setTimestamp(java.time.LocalDateTime.now());
		return postRepository.save(post);
	}

	@Override
	public Post updatePost(User user, Integer postId, Post postDetails) {
		Post post = postRepository.findById(postId)
				.orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + postId));

		if (!post.getUser().getId().equals(user.getId())) {
			throw new ResourceNotFoundException("User is not authorized to update this post.");
		}

		post.setContent(postDetails.getContent());
		post.setImages(postDetails.getImages());
		return postRepository.save(post);
	}

	@Override
	@Transactional
	public void deletePost(User user, Integer postId) {
		Post post = postRepository.findById(postId)
				.orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + postId));

		if (!post.getUser().getId().equals(user.getId())) {
			throw new ResourceNotFoundException("User is not authorized to delete this post.");
		}
		for (User likedUser : post.getLikedUsers()) {
			likedUser.getLikedPosts().remove(post);
			userRepository.save(user);
		}
		postRepository.delete(post);
	}

	@Override
	public Post likePost(Integer postId, User user) {
		Post post = postRepository.findById(postId)
				.orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + postId));
		if (post.getLikedUsers().contains(user)) {
			post.getLikedUsers().remove(user);
			post.setLikes(post.getLikes() - 1);
		} else {
			post.getLikedUsers().add(user);
			post.setLikes(post.getLikes() + 1);
		}

		return postRepository.save(post);
	}

}
