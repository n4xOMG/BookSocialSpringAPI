package com.nix.service.impl;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nix.dtos.PostDTO;
import com.nix.dtos.mappers.PostMapper;
import com.nix.enums.NotificationEntityType;
import com.nix.exception.ForbiddenAccessException;
import com.nix.exception.ResourceNotFoundException;
import com.nix.models.Book;
import com.nix.models.Chapter;
import com.nix.models.Post;
import com.nix.models.Post.PostType;
import com.nix.models.User;
import com.nix.repository.BookRepository;
import com.nix.repository.ChapterRepository;
import com.nix.repository.PostRepository;
import com.nix.repository.UserBlockRepository;
import com.nix.repository.UserRepository;
import com.nix.service.NotificationService;
import com.nix.service.PostService;

@Service
public class PostServiceImpl implements PostService {

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserBlockRepository userBlockRepository;

	@Autowired
	private ChapterRepository chapterRepository;

	@Autowired
	private BookRepository bookRepository;

	@Autowired
	NotificationService notificationService;

	@Autowired
	private PostMapper postMapper;

	@Override
	public Page<PostDTO> getAllPosts(Pageable pageable, User currentUser) {
		Page<Post> postsPage;
		if (currentUser != null) {
			Set<UUID> excludedUserIds = resolveExcludedUserIds(currentUser.getId());
			if (excludedUserIds.isEmpty()) {
				postsPage = postRepository.findAll(pageable);
			} else {
				postsPage = postRepository.findByUser_IdNotIn(List.copyOf(excludedUserIds), pageable);
			}
			return postsPage.map(post -> postMapper.mapToDTO(post, currentUser));
		}
		postsPage = postRepository.findAll(pageable);
		return postsPage.map(postMapper::mapToDTO);
	}

	private Set<UUID> resolveExcludedUserIds(UUID currentUserId) {
		if (currentUserId == null) {
			return Set.of();
		}
		Set<UUID> excluded = new HashSet<>();
		Set<UUID> blockerIds = userBlockRepository.findBlockerIdsByBlockedId(currentUserId);
		if (blockerIds != null) {
			excluded.addAll(blockerIds);
		}
		Set<UUID> blockedIds = userBlockRepository.findBlockedIdsByBlockerId(currentUserId);
		if (blockedIds != null) {
			excluded.addAll(blockedIds);
		}
		return excluded;
	}

	private void ensureNotBlocked(UUID viewerId, UUID ownerId) {
		if (viewerId == null || ownerId == null) {
			return;
		}
		if (userBlockRepository.existsByBlockerIdAndBlockedId(ownerId, viewerId)
				|| userBlockRepository.existsByBlockerIdAndBlockedId(viewerId, ownerId)) {
			throw new ForbiddenAccessException(
					"You cannot interact with this content because one of the accounts has blocked the other.");
		}
	}

	@Override
	public Page<PostDTO> getAllPosts(Pageable pageable) {
		return getAllPosts(pageable, null);
	}

	@Override
	public List<PostDTO> getPostsByUser(User user, User currentUser) {
		if (currentUser != null) {
			ensureNotBlocked(currentUser.getId(), user.getId());
		}
		List<Post> posts = postRepository.findByUser(user);
		if (currentUser != null) {
			return postMapper.mapToDTOs(posts, currentUser);
		} else {
			return postMapper.mapToDTOs(posts);
		}
	}

	@Override
	public List<PostDTO> getPostsByUser(User user) {
		return getPostsByUser(user, null);
	}

	@Override
	public PostDTO getPostById(UUID postId, User currentUser) {
		Post post = postRepository.findById(postId)
				.orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + postId));

		if (currentUser != null) {
			ensureNotBlocked(currentUser.getId(), post.getUser().getId());
		}

		if (currentUser != null) {
			return postMapper.mapToDTO(post, currentUser);
		} else {
			return postMapper.mapToDTO(post);
		}
	}

	@Override
	public PostDTO getPostById(UUID postId) {
		return getPostById(postId, null);
	}

	@Override
	public PostDTO createPost(User user, PostDTO postDTO) {
		Post newPost = new Post();
		newPost.setUser(user);
		newPost.setContent(postDTO.getContent());
		if (postDTO.getImages() != null) {
			newPost.setImages(mapToImages(postDTO.getImages()));
		}
		newPost.setLikes(0);
		newPost.setTimestamp(LocalDateTime.now());
		if (postDTO.getSharedPostId() != null) {
			Post sharedPost = postRepository.findById(postDTO.getSharedPostId())
					.orElseThrow(() -> new ResourceNotFoundException(
							"Cannot find shared post with id: " + postDTO.getSharedPostId()));
			newPost.setSharedPost(sharedPost);
		}

		Post savedPost = postRepository.save(newPost);
		return postMapper.mapToDTO(savedPost, user);
	}

	@Override
	public PostDTO updatePost(User user, UUID postId, PostDTO postDetails) {
		Post post = postRepository.findById(postId)
				.orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + postId));

		if (!post.getUser().getId().equals(user.getId())) {
			throw new ResourceNotFoundException("User is not authorized to update this post.");
		}

		post.setContent(postDetails.getContent());
		post.setImages(mapToImages(postDetails.getImages()));

		Post savedPost = postRepository.save(post);
		return postMapper.mapToDTO(savedPost, user);
	}

	@Override
	@Transactional
	public void deletePost(User user, UUID postId) {
		Post post = postRepository.findById(postId)
				.orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + postId));

		if (!post.getUser().getId().equals(user.getId()) && !post.getUser().getRole().getName().equals("ADMIN")) {
			throw new ResourceNotFoundException("User is not authorized to delete this post.");
		}

		// Clean up shared post references - find all posts that share this post and
		// nullify the reference
		List<Post> sharingPosts = postRepository.findBySharedPost(post);
		for (Post sharingPost : sharingPosts) {
			sharingPost.setSharedPost(null);
		}
		// Save all sharing posts and flush to database
		if (!sharingPosts.isEmpty()) {
			postRepository.saveAllAndFlush(sharingPosts);
		}

		// Clean up liked users references
		post.getLikedUsers().clear();
		postRepository.saveAndFlush(post);

		postRepository.delete(post);
	}

	@Override
	public PostDTO likePost(UUID postId, User user) {
		Post post = postRepository.findById(postId)
				.orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + postId));

		if (userBlockRepository.existsByBlockerIdAndBlockedId(post.getUser().getId(), user.getId())
				|| userBlockRepository.existsByBlockerIdAndBlockedId(user.getId(), post.getUser().getId())) {
			throw new ForbiddenAccessException(
					"You cannot interact with this content because one of the accounts has blocked the other.");
		}

		boolean wasLiked = post.getLikedUsers().contains(user);

		if (wasLiked) {
			post.getLikedUsers().remove(user);
			post.setLikes(post.getLikes() - 1);
		} else {
			post.getLikedUsers().add(user);
			post.setLikes(post.getLikes() + 1);
			String message = "User" + user.getUsername() + " liked your post!";
			notificationService.createNotification(post.getUser(), message, NotificationEntityType.POST, postId);
		}

		Post savedPost = postRepository.save(post);
		return postMapper.mapToDTO(savedPost, user);
	}

	public PostDTO createChapterSharePost(UUID chapterId, User user, PostDTO postDTO) {

		// Find chapter and verify it's published
		Chapter chapter = chapterRepository.findById(chapterId)
				.orElseThrow(() -> new ResourceNotFoundException("Chapter not found"));

		if (chapter.isDraft()) {
			throw new IllegalStateException("Cannot share draft chapters");
		}

		// Create post
		Post post = new Post();
		post.setUser(user);
		post.setContent(postDTO.getContent());
		post.setTimestamp(LocalDateTime.now());
		post.setSharedChapter(chapter);
		post.setSharedBook(chapter.getBook());
		post.setPostType(PostType.CHAPTER_SHARE);

		// Add the book cover as the post image
		if (chapter.getBook().getBookCover() != null) {
			post.getImages().add(chapter.getBook().getBookCover());
		}

		Post savedPost = postRepository.save(post);
		return postMapper.mapToDTO(savedPost);
	}

	// Method to create a post just sharing a book
	public PostDTO createBookSharePost(UUID bookId, User user, PostDTO postDTO) {

		// Find book
		Book book = bookRepository.findById(bookId).orElseThrow(() -> new ResourceNotFoundException("Book not found"));

		// Create post
		Post post = new Post();
		post.setUser(user);
		post.setContent(postDTO.getContent());
		post.setTimestamp(LocalDateTime.now());
		post.setSharedBook(book);
		post.setPostType(PostType.BOOK_SHARE);

		// Add the book cover as the post image
		if (book.getBookCover() != null) {
			post.getImages().add(book.getBookCover());
		}

		Post savedPost = postRepository.save(post);
		return postMapper.mapToDTO(savedPost);
	}

	@Override
	public boolean isPostLikedByCurrentUser(User user, UUID postId) {
		Optional<Post> post = postRepository.findById(postId);
		return post.isPresent() && user.getLikedPosts().contains(post.get());
	}

	private List<com.nix.models.ImageAttachment> mapToImages(List<com.nix.dtos.ImageAttachmentDTO> imageDTOs) {
		if (imageDTOs == null) {
			return new ArrayList<>();
		}
		return imageDTOs.stream().map(dto -> new com.nix.models.ImageAttachment(dto.getUrl(), dto.getIsMild()))
				.collect(Collectors.toList());
	}
}