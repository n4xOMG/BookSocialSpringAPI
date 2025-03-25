package com.nix.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import com.nix.dtos.mappers.CommentMapper;
import com.nix.exception.SensitiveWordException;
import com.nix.models.Comment;
import com.nix.models.User;
import com.nix.service.CommentService;
import com.nix.service.UserService;

@RestController
public class CommentController {

	@Autowired
	CommentService commentService;

	@Autowired
	UserService userService;

	CommentMapper commentMapper = new CommentMapper();

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
	public ResponseEntity<?> getAllPostComments(@PathVariable("postId") Integer postId) {
		try {
			List<Comment> comments = commentService.getAllPostComments(postId);

			return ResponseEntity.ok(commentMapper.mapToDTOs(comments));
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/admin/books/{bookId}/comments")
	public ResponseEntity<?> getAllBookComments(@PathVariable("bookId") Integer bookId) {
		try {
			List<Comment> comments = commentService.getAllBookComments(bookId);

			return ResponseEntity.ok(commentMapper.mapToDTOs(comments));
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/books/{bookId}/comments")
	public ResponseEntity<?> getPagerBookComments(
	        @PathVariable("bookId") Integer bookId,
	        @RequestParam(defaultValue = "0") int page, 
	        @RequestParam(defaultValue = "10") int size) {
	    try {
	        Page<Comment> commentsPage = commentService.getPagerBookComments(page, size, bookId);
	        Map<String, Object> response = new HashMap<>();
	        response.put("comments", commentMapper.mapToDTOs(commentsPage.getContent()));
	        response.put("page", commentsPage.getNumber());
	        response.put("size", commentsPage.getSize());
	        response.put("totalPages", commentsPage.getTotalPages());
	        response.put("totalElements", commentsPage.getTotalElements());
	        
	        return ResponseEntity.ok(response);
	    } catch (Exception e) {
	        return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}

	@GetMapping("/api/chapters/{chapterId}/comments")
	public ResponseEntity<?> getAllChapterComments(@PathVariable("chapterId") Integer chapterId) {
		try {
			List<Comment> comments = commentService.getAllChapterComments(chapterId);

			return ResponseEntity.ok(commentMapper.mapToDTOs(comments));
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/chapters/{chapterId}/comments")
	public ResponseEntity<?> getPagerChapterComments(@PathVariable("chapterId") Integer chapterId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
		try {
			Page<Comment> comments = commentService.getPagerChapterComments(page, size, chapterId);

			return ResponseEntity.ok(commentMapper.mapToDTOs(comments.getContent()));
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private ResponseEntity<?> handleCommentCreation(User user, Supplier<Comment> commentCreator) {
		try {
			if (user == null) {
				return new ResponseEntity<>("User has not logged in!", HttpStatus.UNAUTHORIZED);
			}
			if (user.getIsSuspended()) {
				return new ResponseEntity<>("User is suspended from commenting", HttpStatus.FORBIDDEN);
			}
			Comment newComment = commentCreator.get();
			return ResponseEntity.ok(commentMapper.mapToDTO(newComment));
		} catch (SensitiveWordException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/api/books/{bookId}/comments")
	public ResponseEntity<?> createBookComment(@RequestHeader("Authorization") String jwt, @RequestBody Comment comment,
			@PathVariable("bookId") Integer bookId) {
		User user = userService.findUserByJwt(jwt);
		return handleCommentCreation(user, () -> {
			try {
				return commentService.createBookComment(comment, bookId, user);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return comment;
		});
	}

	@PostMapping("/api/chapters/{chapterId}/comments")
	public ResponseEntity<?> createChapterComment(@RequestHeader("Authorization") String jwt,
			@RequestBody Comment comment, @PathVariable("chapterId") Integer chapterId) {
		User user = userService.findUserByJwt(jwt);
		return handleCommentCreation(user, () -> {
			try {
				return commentService.createChapterComment(comment, chapterId, user);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return comment;
		});
	}

	@PostMapping("/api/posts/{postId}/comments")
	public ResponseEntity<?> createPostComment(@RequestHeader("Authorization") String jwt, @RequestBody Comment comment,
			@PathVariable("postId") Integer postId) {
		User user = userService.findUserByJwt(jwt);
		return handleCommentCreation(user, () -> {
			try {
				return commentService.createPostComment(comment, postId, user);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return comment;
		});
	}

	@PostMapping("/api/books/{bookId}/comments/{parentCommentId}/reply")
	public ResponseEntity<?> createReplyBookComment(@RequestBody Comment comment, @PathVariable Integer parentCommentId,
			@RequestHeader("Authorization") String jwt) {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return new ResponseEntity<>("User has not logged in!", HttpStatus.UNAUTHORIZED);
			}
			Comment replyComment = commentService.createReplyBookComment(comment, parentCommentId, user);
			return new ResponseEntity<>(commentMapper.mapToDTO(replyComment), HttpStatus.CREATED);
		} catch (SensitiveWordException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/api/books/{bookId}/chapters/{chapterId}/comments/{parentCommentId}/reply")
	public ResponseEntity<?> createReplyChapterComment(@RequestBody Comment comment,
			@PathVariable Integer parentCommentId, @RequestHeader("Authorization") String jwt) {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return new ResponseEntity<>("User has not logged in!", HttpStatus.UNAUTHORIZED);
			}
			Comment replyComment = commentService.createReplyChapterComment(comment, parentCommentId, user);
			return new ResponseEntity<>(commentMapper.mapToDTO(replyComment), HttpStatus.CREATED);
		} catch (SensitiveWordException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_ACCEPTABLE);
		}

		catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/api/posts/{postId}/comments/{parentCommentId}/reply")
	public ResponseEntity<?> createReplyPostComment(@RequestBody Comment comment, @PathVariable Integer parentCommentId,
			@RequestHeader("Authorization") String jwt) {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return new ResponseEntity<>("User has not logged in!", HttpStatus.UNAUTHORIZED);
			}
			Comment replyComment = commentService.createReplyPostComment(comment, parentCommentId, user);
			return new ResponseEntity<>(commentMapper.mapToDTO(replyComment), HttpStatus.CREATED);
		} catch (SensitiveWordException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_ACCEPTABLE);
		}

		catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PutMapping("/api/comments/{commentId}/like")
	public ResponseEntity<?> likeComment(@RequestHeader("Authorization") String jwt,
			@PathVariable("commentId") Integer commentId) throws Exception {

		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return new ResponseEntity<>("User has not logged in!", HttpStatus.UNAUTHORIZED);
			}

			if (user.getIsSuspended()) {
				return new ResponseEntity<>("User is suspended from commenting", HttpStatus.FORBIDDEN);
			}

			Boolean isCommentLiked = commentService.likeComment(commentId, user.getId());
			return ResponseEntity.ok(isCommentLiked);

		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PutMapping("/api/comments/{commentId}")
	public ResponseEntity<?> editComment(@RequestHeader("Authorization") String jwt,
			@PathVariable("commentId") Integer commentId, @RequestBody Comment comment) throws Exception {

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
			@PathVariable("commentId") Integer commentId) throws Exception {

		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return new ResponseEntity<>("User has not logged in!", HttpStatus.UNAUTHORIZED);
			}

			return new ResponseEntity<>(commentService.deleteComment(commentId, user.getId()), HttpStatus.OK);

		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

}
