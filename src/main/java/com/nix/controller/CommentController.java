package com.nix.controller;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nix.dtos.BookDTO;
import com.nix.dtos.CommentDTO;
import com.nix.dtos.PostDTO;
import com.nix.dtos.mappers.CommentMapper;
import com.nix.enums.NotificationEntityType;
import com.nix.exception.ForbiddenAccessException;
import com.nix.exception.SensitiveWordException;
import com.nix.exception.UnauthorizedException;
import com.nix.models.Book;
import com.nix.models.Chapter;
import com.nix.models.Comment;
import com.nix.models.User;
import com.nix.service.BookService;
import com.nix.service.ChapterService;
import com.nix.service.CommentService;
import com.nix.service.NotificationService;
import com.nix.service.PostService;
import com.nix.service.UserService;
import com.nix.util.SecurityUtils;
import com.nix.response.ApiResponseWithData;

@RestController
public class CommentController {

	@Autowired
	private CommentService commentService;

	@Autowired
	private UserService userService;

	@Autowired
	private BookService bookService;

	@Autowired
	private ChapterService chapterService;

	@Autowired
	private PostService postService;

	@Autowired
	private NotificationService notificationService;

	private CommentMapper commentMapper = new CommentMapper();

	private User resolveCurrentUser(String jwt) {
		if (jwt == null || jwt.isBlank()) {
			return null;
		}
		return userService.findUserByJwt(jwt);
	}

	private void ensureUserCanComment(User user) {
		if (user.isBanned()) {
			throw new UnauthorizedException("Your account is banned. Contact support for assistance.");
		}
		if (Boolean.TRUE.equals(user.getIsSuspended())) {
			throw new UnauthorizedException(
					"Your account is suspended and won't be able to comment. Contact support for assistance.");
		}
		if (!Boolean.TRUE.equals(user.getIsVerified())) {
			throw new UnauthorizedException("Please verify your account before commenting.");
		}
	}

	private void ensureNotBlocked(User currentUser, UUID ownerId) {
		if (currentUser == null || ownerId == null) {
			return;
		}
		if (SecurityUtils.isAdmin(currentUser)) {
			return;
		}
		if (userService.isBlockedBy(currentUser.getId(), ownerId)
				|| userService.hasBlocked(currentUser.getId(), ownerId)) {
			throw new ForbiddenAccessException(
					"You cannot access this resource because one of the accounts has blocked the other.");
		}
	}

	private CommentDTO filterCommentTree(CommentDTO comment, Set<UUID> blockerIds) {
		if (comment == null) {
			return null;
		}
		if (comment.getUser() != null && blockerIds.contains(comment.getUser().getId())) {
			return null;
		}
		if (comment.getReplyComment() != null) {
			List<CommentDTO> filteredReplies = comment.getReplyComment().stream()
					.map(reply -> filterCommentTree(reply, blockerIds))
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
			comment.setReplyComment(filteredReplies);
		}
		return comment;
	}

	private List<CommentDTO> filterCommentsForUser(List<CommentDTO> comments, User currentUser) {
		if (currentUser == null || comments == null) {
			return comments;
		}
		if (SecurityUtils.isAdmin(currentUser)) {
			return comments;
		}
		Set<UUID> hiddenUserIds = new HashSet<>(userService.getUserIdsBlocking(currentUser.getId()));
		hiddenUserIds.addAll(userService.getBlockedUserIds(currentUser.getId()));
		if (hiddenUserIds.isEmpty()) {
			return comments;
		}
		return comments.stream().map(comment -> filterCommentTree(comment, hiddenUserIds)).filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	private void ensureNotBlockedForComment(User currentUser, Comment comment) {
		if (currentUser == null || comment == null) {
			return;
		}
		if (comment.getUser() != null) {
			ensureNotBlocked(currentUser, comment.getUser().getId());
		}
		if (comment.getBook() != null && comment.getBook().getAuthor() != null) {
			ensureNotBlocked(currentUser, comment.getBook().getAuthor().getId());
		}
		if (comment.getChapter() != null && comment.getChapter().getBook() != null
				&& comment.getChapter().getBook().getAuthor() != null) {
			ensureNotBlocked(currentUser, comment.getChapter().getBook().getAuthor().getId());
		}
		if (comment.getPost() != null && comment.getPost().getUser() != null) {
			ensureNotBlocked(currentUser, comment.getPost().getUser().getId());
		}
		if (comment.getParentComment() != null && comment.getParentComment().getUser() != null) {
			ensureNotBlocked(currentUser, comment.getParentComment().getUser().getId());
		}
	}

	@GetMapping("/api/comments")
	public ResponseEntity<ApiResponseWithData<List<CommentDTO>>> getAllComments() {
		try {
			List<Comment> comments = commentService.getAllComments();
			List<CommentDTO> commentDTOs = commentMapper.mapToDTOs(comments);
			return buildSuccessResponse("Comments retrieved successfully.", commentDTOs);
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildErrorResponse(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(new ApiResponseWithData<>(message, false));
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildSuccessResponse(String message, T data) {
		return buildSuccessResponse(HttpStatus.OK, message, data);
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildSuccessResponse(HttpStatus status, String message, T data) {
		return ResponseEntity.status(status).body(new ApiResponseWithData<>(message, true, data));
	}

	@GetMapping("/posts/{postId}/comments")
	public ResponseEntity<ApiResponseWithData<Map<String, Object>>> getAllPostComments(
			@PathVariable("postId") UUID postId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestHeader(value = "Authorization", required = false) String jwt) {
		try {
			User currentUser = resolveCurrentUser(jwt);
			if (currentUser != null) {
				PostDTO post = postService.getPostById(postId);
				if (post != null && post.getUser() != null) {
					ensureNotBlocked(currentUser, post.getUser().getId());
				}
			}
			Page<Comment> commentsPage = commentService.getPagerPostComments(page, size, postId);
			List<CommentDTO> commentDTOs = commentMapper.mapToDTOs(commentsPage.getContent());
			commentDTOs = filterCommentsForUser(commentDTOs, currentUser);

			if (currentUser != null) {
				for (CommentDTO comment : commentDTOs) {
					setLikedByCurrentUserRecursively(comment, currentUser, commentService);
				}
			}

			Map<String, Object> response = new HashMap<>();
			response.put("comments", commentDTOs);
			response.put("page", commentsPage.getNumber());
			response.put("size", commentsPage.getSize());
			response.put("totalPages", commentsPage.getTotalPages());
			response.put("totalElements", commentsPage.getTotalElements());

			return buildSuccessResponse("Comments retrieved successfully.", response);

		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@GetMapping("/books/{bookId}/comments")
	public ResponseEntity<ApiResponseWithData<Map<String, Object>>> getPagerBookComments(
			@PathVariable("bookId") UUID bookId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestHeader(value = "Authorization", required = false) String jwt) {
		try {
			User currentUser = resolveCurrentUser(jwt);
			if (currentUser != null) {
				BookDTO book = bookService.getBookById(bookId);
				if (book != null && book.getAuthor() != null) {
					ensureNotBlocked(currentUser, book.getAuthor().getId());
				}
			}
			Page<Comment> commentsPage = commentService.getPagerBookComments(page, size, bookId);
			List<CommentDTO> commentDTOs = commentMapper.mapToDTOs(commentsPage.getContent());
			commentDTOs = filterCommentsForUser(commentDTOs, currentUser);

			if (currentUser != null) {
				for (CommentDTO comment : commentDTOs) {
					setLikedByCurrentUserRecursively(comment, currentUser, commentService);
				}
			}

			Map<String, Object> response = new HashMap<>();
			response.put("comments", commentDTOs);
			response.put("page", commentsPage.getNumber());
			response.put("size", commentsPage.getSize());
			response.put("totalPages", commentsPage.getTotalPages());
			response.put("totalElements", commentsPage.getTotalElements());

			return buildSuccessResponse("Comments retrieved successfully.", response);
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	private void setLikedByCurrentUserRecursively(CommentDTO comment, User user, CommentService commentService) {
		comment.setLikedByCurrentUser(commentService.isCommentLikedByCurrentUser(comment.getId(), user));
		if (comment.getReplyComment() != null && !comment.getReplyComment().isEmpty()) {
			for (CommentDTO reply : comment.getReplyComment()) {
				setLikedByCurrentUserRecursively(reply, user, commentService);
			}
		}
	}

	@GetMapping("/chapters/{chapterId}/comments")
	public ResponseEntity<ApiResponseWithData<Map<String, Object>>> getPagerChapterComments(
			@PathVariable("chapterId") UUID chapterId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestHeader(value = "Authorization", required = false) String jwt) {
		try {
			User currentUser = resolveCurrentUser(jwt);
			if (currentUser != null) {
				Chapter chapter = chapterService.findChapterById(chapterId);
				if (chapter != null && chapter.getBook() != null && chapter.getBook().getAuthor() != null) {
					ensureNotBlocked(currentUser, chapter.getBook().getAuthor().getId());
				}
			}
			Page<Comment> comments = commentService.getPagerChapterComments(page, size, chapterId);
			List<CommentDTO> commentDTOs = commentMapper.mapToDTOs(comments.getContent());
			commentDTOs = filterCommentsForUser(commentDTOs, currentUser);

			if (currentUser != null) {
				for (CommentDTO comment : commentDTOs) {
					setLikedByCurrentUserRecursively(comment, currentUser, commentService);
				}
			}

			Map<String, Object> response = new HashMap<>();
			response.put("comments", commentDTOs);
			response.put("page", comments.getNumber());
			response.put("size", comments.getSize());
			response.put("totalPages", comments.getTotalPages());
			response.put("totalElements", comments.getTotalElements());

			return buildSuccessResponse("Comments retrieved successfully.", response);
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@GetMapping("/admin/comments/recent/{userId}")
	@PreAuthorize("hasAnyRole('ADMIN')")
	public ResponseEntity<ApiResponseWithData<List<CommentDTO>>> getRecentCommentsByUserId(@PathVariable UUID userId) {
		try {
			Page<Comment> comments = commentService.getRecentCommentsByUserId(userId, 0, 5);
			List<CommentDTO> commentDTOs = commentMapper.mapToDTOs(comments.getContent());
			return buildSuccessResponse("Comments retrieved successfully.", commentDTOs);
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	private ResponseEntity<ApiResponseWithData<CommentDTO>> handleCommentCreation(User user,
			Supplier<Comment> commentCreator, String context, UUID entityId) {
		try {
			if (user == null) {
				return buildErrorResponse(HttpStatus.UNAUTHORIZED,
						"Authentication is required to perform this action.");
			}
			ensureUserCanComment(user);

			BookDTO book = null;
			Chapter chapter = null;
			PostDTO post = null;

			if ("book".equals(context) && entityId != null) {
				book = bookService.getBookById(entityId);
				if (book != null && book.getAuthor() != null) {
					ensureNotBlocked(user, book.getAuthor().getId());
				}
			} else if ("chapter".equals(context) && entityId != null) {
				chapter = chapterService.findChapterById(entityId);
				if (chapter != null && chapter.getBook() != null && chapter.getBook().getAuthor() != null) {
					ensureNotBlocked(user, chapter.getBook().getAuthor().getId());
				}
			} else if ("post".equals(context) && entityId != null) {
				post = postService.getPostById(entityId);
				if (post != null && post.getUser() != null) {
					ensureNotBlocked(user, post.getUser().getId());
				}
			}
			Comment newComment = commentCreator.get();

			// Notify relevant user based on context
			if ("book".equals(context) && entityId != null) {

				if (book == null) {
					book = bookService.getBookById(entityId);
				}
				UUID authorId = book.getAuthor().getId();
				User author = userService.findUserById(authorId);

				if (author != null && !author.equals(user)) { // Don't notify if commenter is the author
					notificationService.createNotification(author, "A new comment was posted on your book '"
							+ book.getTitle() + "': " + newComment.getContent(), NotificationEntityType.COMMENT,
							book.getId());
				}
			} else if ("chapter".equals(context) && entityId != null) {

				if (chapter == null) {
					chapter = chapterService.findChapterById(entityId);
				}
				User author = chapter.getBook().getAuthor();
				if (author != null && !author.equals(user)) {
					notificationService.createNotification(
							author, "A new comment was posted on a chapter of your book '"
									+ chapter.getBook().getTitle() + "': " + newComment.getContent(),
							NotificationEntityType.COMMENT, chapter.getId());
				}
			} else if ("post".equals(context) && entityId != null) {

				if (post == null) {
					post = postService.getPostById(entityId);
				}
				UUID authorId = post.getUser().getId();
				User author = userService.findUserById(authorId);

				if (post.getUser() != null && !author.equals(user)) {
					notificationService.createNotification(author,
							"A new comment was posted on your post: " + newComment.getContent(),
							NotificationEntityType.COMMENT, post.getId());
				}
			}

			return buildSuccessResponse(HttpStatus.CREATED, "Comment created successfully.",
					commentMapper.mapToDTO(newComment));
		} catch (SensitiveWordException e) {
			return buildErrorResponse(HttpStatus.NOT_ACCEPTABLE, e.getMessage());
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@PostMapping("/api/books/{bookId}/comments")
	public ResponseEntity<ApiResponseWithData<CommentDTO>> createBookComment(@RequestHeader("Authorization") String jwt,
			@RequestBody Comment comment,
			@PathVariable("bookId") UUID bookId) {
		User user = resolveCurrentUser(jwt);
		return handleCommentCreation(user, () -> {
			try {
				return commentService.createBookComment(comment, bookId, user);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return comment;
		}, "book", bookId);
	}

	@PostMapping("/api/chapters/{chapterId}/comments")
	public ResponseEntity<ApiResponseWithData<CommentDTO>> createChapterComment(
			@RequestHeader("Authorization") String jwt,
			@RequestBody Comment comment, @PathVariable("chapterId") UUID chapterId) {
		User user = resolveCurrentUser(jwt);
		return handleCommentCreation(user, () -> {
			try {
				return commentService.createChapterComment(comment, chapterId, user);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return comment;
		}, "chapter", chapterId);
	}

	@PostMapping("/api/posts/{postId}/comments")
	public ResponseEntity<ApiResponseWithData<CommentDTO>> createPostComment(@RequestHeader("Authorization") String jwt,
			@RequestBody Comment comment,
			@PathVariable("postId") UUID postId) {
		User user = resolveCurrentUser(jwt);
		return handleCommentCreation(user, () -> {
			try {
				return commentService.createPostComment(comment, postId, user);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return comment;
		}, "post", postId);
	}

	@PostMapping("/api/books/{bookId}/comments/{parentCommentId}/reply")
	public ResponseEntity<ApiResponseWithData<CommentDTO>> createReplyBookComment(@RequestBody Comment comment,
			@PathVariable UUID parentCommentId,
			@RequestHeader("Authorization") String jwt) {
		try {
			User user = resolveCurrentUser(jwt);
			if (user == null) {
				return buildErrorResponse(HttpStatus.UNAUTHORIZED,
						"Authentication is required to perform this action.");
			}
			ensureUserCanComment(user);
			Comment parentComment = commentService.findCommentById(parentCommentId);
			ensureNotBlockedForComment(user, parentComment);
			Comment replyComment = commentService.createReplyBookComment(comment, parentCommentId, user);

			// Notify the parent comment's author
			User parentAuthor = parentComment.getUser();
			if (parentAuthor != null && !parentAuthor.equals(user)) {
				BookDTO book = bookService.getBookById(parentComment.getBook().getId());
				notificationService.createNotification(parentAuthor,
						"Someone replied to your comment on '" + book.getTitle() + "': " + replyComment.getContent(),
						NotificationEntityType.COMMENT, comment.getId());
			}

			return buildSuccessResponse(HttpStatus.CREATED, "Reply created successfully.",
					commentMapper.mapToDTO(replyComment));
		} catch (SensitiveWordException e) {
			return buildErrorResponse(HttpStatus.NOT_ACCEPTABLE, e.getMessage());
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@PostMapping("/api/chapters/{chapterId}/comments/{parentCommentId}/reply")
	public ResponseEntity<ApiResponseWithData<CommentDTO>> createReplyChapterComment(@RequestBody Comment comment,
			@PathVariable UUID parentCommentId,
			@RequestHeader("Authorization") String jwt) {
		try {
			User user = resolveCurrentUser(jwt);
			if (user == null) {
				return buildErrorResponse(HttpStatus.UNAUTHORIZED,
						"Authentication is required to perform this action.");
			}
			ensureUserCanComment(user);
			Comment parentComment = commentService.findCommentById(parentCommentId);
			ensureNotBlockedForComment(user, parentComment);
			Comment replyComment = commentService.createReplyChapterComment(comment, parentCommentId, user);

			// Notify the parent comment's author
			User parentAuthor = parentComment.getUser();
			if (parentAuthor != null && !parentAuthor.equals(user)) {
				Book book = parentComment.getChapter().getBook();
				notificationService
						.createNotification(
								parentAuthor, "Someone replied to your comment on a chapter of '" + book.getTitle()
										+ "': " + replyComment.getContent(),
								NotificationEntityType.COMMENT, parentCommentId);
			}

			return buildSuccessResponse(HttpStatus.CREATED, "Reply created successfully.",
					commentMapper.mapToDTO(replyComment));
		} catch (SensitiveWordException e) {
			return buildErrorResponse(HttpStatus.NOT_ACCEPTABLE, e.getMessage());
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@PostMapping("/api/posts/{postId}/comments/{parentCommentId}/reply")
	public ResponseEntity<ApiResponseWithData<CommentDTO>> createReplyPostComment(@RequestBody Comment comment,
			@PathVariable UUID parentCommentId,
			@RequestHeader("Authorization") String jwt) {
		try {
			User user = resolveCurrentUser(jwt);
			if (user == null) {
				return buildErrorResponse(HttpStatus.UNAUTHORIZED,
						"Authentication is required to perform this action.");
			}
			ensureUserCanComment(user);
			Comment parentComment = commentService.findCommentById(parentCommentId);
			ensureNotBlockedForComment(user, parentComment);
			Comment replyComment = commentService.createReplyPostComment(comment, parentCommentId, user);

			// Notify the parent comment's author
			User parentAuthor = parentComment.getUser();
			if (parentAuthor != null && !parentAuthor.equals(user)) {
				notificationService.createNotification(parentAuthor,
						"Someone replied to your comment on a post: " + replyComment.getContent(),
						NotificationEntityType.COMMENT, parentCommentId);
			}

			return buildSuccessResponse(HttpStatus.CREATED, "Reply created successfully.",
					commentMapper.mapToDTO(replyComment));
		} catch (SensitiveWordException e) {
			return buildErrorResponse(HttpStatus.NOT_ACCEPTABLE, e.getMessage());
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@PutMapping("/api/comments/{commentId}/like")
	public ResponseEntity<ApiResponseWithData<CommentDTO>> likeComment(@RequestHeader("Authorization") String jwt,
			@PathVariable("commentId") UUID commentId) throws Exception {
		try {
			User user = resolveCurrentUser(jwt);
			if (user == null) {
				return buildErrorResponse(HttpStatus.UNAUTHORIZED,
						"Authentication is required to perform this action.");
			}
			ensureUserCanComment(user);
			Comment comment = commentService.findCommentById(commentId);
			ensureNotBlockedForComment(user, comment);

			Boolean isCommentLiked = commentService.likeComment(commentId, user.getId());
			Comment updatedComment = commentService.findCommentById(commentId);
			CommentDTO commentDTO = commentMapper.mapToDTO(updatedComment);
			commentDTO.setLikedByCurrentUser(isCommentLiked);

			// Notify the comment's author if liked
			User commentAuthor = updatedComment.getUser();
			if (commentAuthor != null && !commentAuthor.equals(user) && isCommentLiked) {
				String context = updatedComment.getBook() != null
						? "book '" + updatedComment.getBook().getTitle() + "'"
						: updatedComment.getChapter() != null ? "chapter"
								: updatedComment.getPost() != null ? "post" : "content";
				notificationService.createNotification(commentAuthor,
						"Your comment on " + context + " was liked: " + updatedComment.getContent(),
						NotificationEntityType.COMMENT, commentId);
			}

			String message = Boolean.TRUE.equals(isCommentLiked) ? "Comment liked successfully."
					: "Comment unliked successfully.";
			return buildSuccessResponse(message, commentDTO);
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@PutMapping("/api/comments/{commentId}")
	public ResponseEntity<ApiResponseWithData<CommentDTO>> editComment(@RequestHeader("Authorization") String jwt,
			@PathVariable("commentId") UUID commentId, @RequestBody Comment comment) throws Exception {
		try {
			User user = resolveCurrentUser(jwt);
			if (user == null) {
				return buildErrorResponse(HttpStatus.UNAUTHORIZED,
						"Authentication is required to perform this action.");
			}
			ensureUserCanComment(user);
			Comment existingComment = commentService.findCommentById(commentId);
			ensureNotBlockedForComment(user, existingComment);
			Comment editedComment = commentService.editComment(user.getId(), commentId, comment);

			return buildSuccessResponse("Comment updated successfully.", commentMapper.mapToDTO(editedComment));
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@DeleteMapping("/api/comments/{commentId}")
	public ResponseEntity<ApiResponseWithData<UUID>> deleteComment(@RequestHeader("Authorization") String jwt,
			@PathVariable("commentId") UUID commentId) throws Exception {
		try {
			User user = resolveCurrentUser(jwt);
			if (user == null) {
				return buildErrorResponse(HttpStatus.UNAUTHORIZED,
						"Authentication is required to perform this action.");
			}
			ensureUserCanComment(user);

			Comment comment = commentService.findCommentById(commentId); // Fetch before deletion
			ensureNotBlockedForComment(user, comment);
			commentService.deleteComment(commentId, user.getId());

			// Notify the comment's author or parent author if deleted by someone else
			User commentAuthor = comment.getUser();
			if (commentAuthor != null && !commentAuthor.equals(user)) {
				String context = comment.getBook() != null ? "book '" + comment.getBook().getTitle() + "'"
						: comment.getChapter() != null ? "chapter" : comment.getPost() != null ? "post" : "content";
				notificationService.createNotification(commentAuthor,
						"Your comment on " + context + " was deleted: " + comment.getContent(),
						NotificationEntityType.COMMENT, commentId);
			}

			return buildSuccessResponse("Comment deleted successfully.", commentId);
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}
}