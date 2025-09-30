package com.nix.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import com.nix.exception.SensitiveWordException;
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

	@GetMapping("/api/comments")
	public ResponseEntity<?> getAllComments() {
		try {
			List<Comment> comments = commentService.getAllComments();
			return ResponseEntity.ok(commentMapper.mapToDTOs(comments));
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/posts/{postId}/comments")
	public ResponseEntity<?> getAllPostComments(@PathVariable("postId") UUID postId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestHeader(value = "Authorization", required = false) String jwt) {
		try {
			Page<Comment> commentsPage = commentService.getPagerPostComments(page, size, postId);
			List<CommentDTO> commentDTOs = commentMapper.mapToDTOs(commentsPage.getContent());

			if (jwt != null && !jwt.isEmpty()) {
				User user = userService.findUserByJwt(jwt);
				for (CommentDTO comment : commentDTOs) {
					setLikedByCurrentUserRecursively(comment, user, commentService);
				}
			}

			Map<String, Object> response = new HashMap<>();
			response.put("comments", commentDTOs);
			response.put("page", commentsPage.getNumber());
			response.put("size", commentsPage.getSize());
			response.put("totalPages", commentsPage.getTotalPages());
			response.put("totalElements", commentsPage.getTotalElements());

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/books/{bookId}/comments")
	public ResponseEntity<?> getPagerBookComments(@PathVariable("bookId") UUID bookId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestHeader(value = "Authorization", required = false) String jwt) {
		try {
			Page<Comment> commentsPage = commentService.getPagerBookComments(page, size, bookId);
			List<CommentDTO> commentDTOs = commentMapper.mapToDTOs(commentsPage.getContent());

			if (jwt != null && !jwt.isEmpty()) {
				User user = userService.findUserByJwt(jwt);
				for (CommentDTO comment : commentDTOs) {
					setLikedByCurrentUserRecursively(comment, user, commentService);
				}
			}

			Map<String, Object> response = new HashMap<>();
			response.put("comments", commentDTOs);
			response.put("page", commentsPage.getNumber());
			response.put("size", commentsPage.getSize());
			response.put("totalPages", commentsPage.getTotalPages());
			response.put("totalElements", commentsPage.getTotalElements());

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
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
	public ResponseEntity<?> getPagerChapterComments(@PathVariable("chapterId") UUID chapterId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestHeader(value = "Authorization", required = false) String jwt) {
		try {
			Page<Comment> comments = commentService.getPagerChapterComments(page, size, chapterId);
			List<CommentDTO> commentDTOs = commentMapper.mapToDTOs(comments.getContent());

			if (jwt != null && !jwt.isEmpty()) {
				User user = userService.findUserByJwt(jwt);
				for (CommentDTO comment : commentDTOs) {
					comment.setLikedByCurrentUser(commentService.isCommentLikedByCurrentUser(comment.getId(), user));
				}
			}

			Map<String, Object> response = new HashMap<>();
			response.put("comments", commentDTOs);
			response.put("page", comments.getNumber());
			response.put("size", comments.getSize());
			response.put("totalPages", comments.getTotalPages());
			response.put("totalElements", comments.getTotalElements());

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/admin/comments/recent/{userId}")
	public ResponseEntity<?> getRecentCommentsByUserId(@PathVariable UUID userId) {
		try {
			Page<Comment> comments = commentService.getRecentCommentsByUserId(userId, 0, 5);
			return ResponseEntity.ok(commentMapper.mapToDTOs(comments.getContent()));
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private ResponseEntity<?> handleCommentCreation(User user, Supplier<Comment> commentCreator, String context,
			UUID entityId) {
		try {
			if (user == null) {
				return new ResponseEntity<>("User has not logged in!", HttpStatus.UNAUTHORIZED);
			}
			if (user.getIsSuspended()) {
				return new ResponseEntity<>("User is suspended from commenting", HttpStatus.FORBIDDEN);
			}
			Comment newComment = commentCreator.get();

			// Notify relevant user based on context
			if ("book".equals(context) && entityId != null) {

				BookDTO book = bookService.getBookById(entityId);
				UUID authorId = book.getAuthor().getId();
				User author = userService.findUserById(authorId);

				if (author != null && !author.equals(user)) { // Don't notify if commenter is the author
					notificationService.createNotification(author, "A new comment was posted on your book '"
							+ book.getTitle() + "': " + newComment.getContent(), NotificationEntityType.COMMENT,
							book.getId());
				}
			} else if ("chapter".equals(context) && entityId != null) {

				Chapter chapter = chapterService.findChapterById(entityId);
				User author = chapter.getBook().getAuthor();
				if (author != null && !author.equals(user)) {
					notificationService.createNotification(
							author, "A new comment was posted on a chapter of your book '"
									+ chapter.getBook().getTitle() + "': " + newComment.getContent(),
							NotificationEntityType.COMMENT, chapter.getId());
				}
			} else if ("post".equals(context) && entityId != null) {

				PostDTO post = postService.getPostById(entityId);
				UUID authorId = post.getUser().getId();
				User author = userService.findUserById(authorId);

				if (post.getUser() != null && !author.equals(user)) {
					notificationService.createNotification(author,
							"A new comment was posted on your post: " + newComment.getContent(),
							NotificationEntityType.COMMENT, post.getId());
				}
			}

			return ResponseEntity.ok(commentMapper.mapToDTO(newComment));
		} catch (SensitiveWordException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/api/books/{bookId}/comments")
	public ResponseEntity<?> createBookComment(@RequestHeader("Authorization") String jwt, @RequestBody Comment comment,
			@PathVariable("bookId") UUID bookId) {
		User user = userService.findUserByJwt(jwt);
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
	public ResponseEntity<?> createChapterComment(@RequestHeader("Authorization") String jwt,
			@RequestBody Comment comment, @PathVariable("chapterId") UUID chapterId) {
		User user = userService.findUserByJwt(jwt);
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
	public ResponseEntity<?> createPostComment(@RequestHeader("Authorization") String jwt, @RequestBody Comment comment,
			@PathVariable("postId") UUID postId) {
		User user = userService.findUserByJwt(jwt);
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
	public ResponseEntity<?> createReplyBookComment(@RequestBody Comment comment, @PathVariable UUID parentCommentId,
			@RequestHeader("Authorization") String jwt) {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return new ResponseEntity<>("User has not logged in!", HttpStatus.UNAUTHORIZED);
			}
			Comment replyComment = commentService.createReplyBookComment(comment, parentCommentId, user);

			// Notify the parent comment's author
			Comment parentComment = commentService.findCommentById(parentCommentId);
			User parentAuthor = parentComment.getUser();
			if (parentAuthor != null && !parentAuthor.equals(user)) {
				BookDTO book = bookService.getBookById(parentComment.getBook().getId());
				notificationService.createNotification(parentAuthor,
						"Someone replied to your comment on '" + book.getTitle() + "': " + replyComment.getContent(),
						NotificationEntityType.COMMENT, comment.getId());
			}

			return new ResponseEntity<>(commentMapper.mapToDTO(replyComment), HttpStatus.CREATED);
		} catch (SensitiveWordException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/api/chapters/{chapterId}/comments/{parentCommentId}/reply")
	public ResponseEntity<?> createReplyChapterComment(@RequestBody Comment comment, @PathVariable UUID parentCommentId,
			@RequestHeader("Authorization") String jwt) {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return new ResponseEntity<>("User has not logged in!", HttpStatus.UNAUTHORIZED);
			}
			Comment replyComment = commentService.createReplyChapterComment(comment, parentCommentId, user);

			// Notify the parent comment's author
			Comment parentComment = commentService.findCommentById(parentCommentId);
			User parentAuthor = parentComment.getUser();
			if (parentAuthor != null && !parentAuthor.equals(user)) {
				Book book = parentComment.getChapter().getBook();
				notificationService
						.createNotification(
								parentAuthor, "Someone replied to your comment on a chapter of '" + book.getTitle()
										+ "': " + replyComment.getContent(),
								NotificationEntityType.COMMENT, parentCommentId);
			}

			return new ResponseEntity<>(commentMapper.mapToDTO(replyComment), HttpStatus.CREATED);
		} catch (SensitiveWordException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/api/posts/{postId}/comments/{parentCommentId}/reply")
	public ResponseEntity<?> createReplyPostComment(@RequestBody Comment comment, @PathVariable UUID parentCommentId,
			@RequestHeader("Authorization") String jwt) {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return new ResponseEntity<>("User has not logged in!", HttpStatus.UNAUTHORIZED);
			}
			Comment replyComment = commentService.createReplyPostComment(comment, parentCommentId, user);

			// Notify the parent comment's author
			Comment parentComment = commentService.findCommentById(parentCommentId);
			User parentAuthor = parentComment.getUser();
			if (parentAuthor != null && !parentAuthor.equals(user)) {
				notificationService.createNotification(parentAuthor,
						"Someone replied to your comment on a post: " + replyComment.getContent(),
						NotificationEntityType.COMMENT, parentCommentId);
			}

			return new ResponseEntity<>(commentMapper.mapToDTO(replyComment), HttpStatus.CREATED);
		} catch (SensitiveWordException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PutMapping("/api/comments/{commentId}/like")
	public ResponseEntity<?> likeComment(@RequestHeader("Authorization") String jwt,
			@PathVariable("commentId") UUID commentId) throws Exception {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return new ResponseEntity<>("User has not logged in!", HttpStatus.UNAUTHORIZED);
			}
			if (user.getIsSuspended()) {
				return new ResponseEntity<>("User is suspended from commenting", HttpStatus.FORBIDDEN);
			}

			Boolean isCommentLiked = commentService.likeComment(commentId, user.getId());
			Comment comment = commentService.findCommentById(commentId);
			CommentDTO commentDTO = commentMapper.mapToDTO(comment);
			commentDTO.setLikedByCurrentUser(isCommentLiked);

			// Notify the comment's author if liked
			User commentAuthor = comment.getUser();
			if (commentAuthor != null && !commentAuthor.equals(user) && isCommentLiked) {
				String context = comment.getBook() != null ? "book '" + comment.getBook().getTitle() + "'"
						: comment.getChapter() != null ? "chapter" : comment.getPost() != null ? "post" : "content";
				notificationService.createNotification(commentAuthor,
						"Your comment on " + context + " was liked: " + comment.getContent(),
						NotificationEntityType.COMMENT, commentId);
			}

			return ResponseEntity.ok(commentDTO);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PutMapping("/api/comments/{commentId}")
	public ResponseEntity<?> editComment(@RequestHeader("Authorization") String jwt,
			@PathVariable("commentId") UUID commentId, @RequestBody Comment comment) throws Exception {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return new ResponseEntity<>("User has not logged in!", HttpStatus.UNAUTHORIZED);
			}
			Comment editedComment = commentService.editComment(user.getId(), commentId, comment);

			return new ResponseEntity<>(commentMapper.mapToDTO(editedComment), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@DeleteMapping("/api/comments/{commentId}")
	public ResponseEntity<?> deleteComment(@RequestHeader("Authorization") String jwt,
			@PathVariable("commentId") UUID commentId) throws Exception {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return new ResponseEntity<>("User has not logged in!", HttpStatus.UNAUTHORIZED);
			}

			Comment comment = commentService.findCommentById(commentId); // Fetch besfore deletion
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

			return new ResponseEntity<>(commentId, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}