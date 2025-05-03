package com.nix.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.nix.models.Comment;
import com.nix.models.User;

public interface CommentService {
	public List<Comment> getAllComments();

	public List<Comment> getAllParentComments();

	public Page<Comment> getPagerBookComments(int page, int size, Integer bookId);

	public Page<Comment> getPagerChapterComments(int page, int size, Integer chapterId);

	public Page<Comment> getPagerPostComments(int page, int size, Integer postId);
	
	public Page<Comment> getRecentCommentsByUserId(Integer userId, int page, int size);

	public Comment createPostComment(Comment comment, Integer postId, User user) throws Exception;

	public Comment createBookComment(Comment comment, Integer bookId, User user) throws Exception;

	public Comment createChapterComment(Comment comment, Integer chapterId, User user) throws Exception;

	public Boolean likeComment(Integer commentId, Integer userId) throws Exception;
	
	public Boolean isCommentLikedByCurrentUser(Integer commentId, User user);

	public Comment findCommentById(Integer commentId) throws Exception;

	public String deleteComment(Integer commentId, Integer userId) throws Exception;

	public Comment editComment(Integer userId, Integer commentId, Comment comment) throws Exception;

	public Comment createReplyBookComment(Comment comment, Integer parentCommentId, User user) throws Exception;

	public Comment createReplyChapterComment(Comment comment, Integer parentCommentId, User user) throws Exception;

	public Comment createReplyPostComment(Comment comment, Integer parentCommentId, User user) throws Exception;
}
