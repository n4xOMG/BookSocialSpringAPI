package com.nix.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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

	@GetMapping("/books/{bookId}/comments")
	public ResponseEntity<?> getAllBookComments(@PathVariable("bookId") Integer bookId) {
		try {
			List<Comment> comments = commentService.getAllBookComments(bookId);

			return ResponseEntity.ok(commentMapper.mapToDTOs(comments));
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/books/{bookId}/chapters/{chapterId}/comments")
	public ResponseEntity<?> getAllChapterComments(@PathVariable("chapterId") Integer chapterId) {
		try {
			List<Comment> comments = commentService.getAllChapterComments(chapterId);

			return ResponseEntity.ok(commentMapper.mapToDTOs(comments));
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/api/books/{bookId}/comments")
	public ResponseEntity<?> createBookComment(@RequestHeader("Authorization") String jwt, @RequestBody Comment comment,
			@PathVariable("bookId") Integer bookId) {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return new ResponseEntity<>("User has not logged in!", HttpStatus.UNAUTHORIZED);
			}

			if (user.getIsSuspended()) {
				return new ResponseEntity<>("User is suspended from commenting", HttpStatus.FORBIDDEN);
			}

			Comment newComment = commentService.createBookComment(comment, bookId, user.getId());
			return ResponseEntity.ok(commentMapper.mapToDTO(newComment));

		} catch (SensitiveWordException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/api/books/{bookId}/chapters/{chapterId}/comments")
	public ResponseEntity<?> createChapterComment(@RequestHeader("Authorization") String jwt,
			@RequestBody Comment comment, @PathVariable("bookId") Integer bookId,
			@PathVariable("chapterId") Integer chapterId) throws Exception {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return new ResponseEntity<>("User has not logged in!", HttpStatus.UNAUTHORIZED);
			}

			if (user.getIsSuspended()) {
				return new ResponseEntity<>("User is suspended from commenting", HttpStatus.FORBIDDEN);
			}

			Comment newComment = commentService.createChapterComment(comment, bookId, chapterId, user.getId());
			return ResponseEntity.ok(commentMapper.mapToDTO(newComment));

		} catch (SensitiveWordException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@PostMapping("/api/books/{bookId}/comments/{parentCommentId}/reply")
	public ResponseEntity<?> createReplyBookComment(@RequestBody Comment comment, @PathVariable Integer parentCommentId,
			@RequestHeader("Authorization") String jwt) {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return new ResponseEntity<>("User has not logged in!", HttpStatus.UNAUTHORIZED);
			}
			Comment replyComment = commentService.createReplyBookComment(comment, parentCommentId, user.getId());
			return new ResponseEntity<>(replyComment, HttpStatus.CREATED);
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
			Comment replyComment = commentService.createReplyChapterComment(comment, parentCommentId, user.getId());
			return new ResponseEntity<>(replyComment, HttpStatus.CREATED);
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

			Comment comment = commentService.likeComment(commentId, user.getId());
			return ResponseEntity.ok(commentMapper.mapToDTO(comment));

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
