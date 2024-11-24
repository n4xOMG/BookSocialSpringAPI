package com.nix.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nix.models.Comment;

public interface CommentRepository extends JpaRepository<Comment, Integer> {
	public List<Comment> findByUserId(Integer userId);

	public List<Comment> findByBookId(Integer bookId);

	public List<Comment> findByChapterId(Integer chapterId);

	@Query("select c from Comment c where c.parentComment IS NULL")
	public List<Comment> findParentComments();

	@Query("SELECT c FROM Comment c WHERE c.parentComment IS NULL AND c.book.id = :bookId AND c.chapter.id IS NULL")
	public List<Comment> findParentCommentsByBookId(@Param("bookId") Integer bookId);

	@Query("SELECT c FROM Comment c WHERE c.parentComment IS NULL AND c.chapter.id = :chapterId")
	public List<Comment> findParentCommentsByChapterId(@Param("chapterId") Integer chapterId);

	@Query("SELECT c FROM Comment c WHERE c.parentComment IS NULL AND c.post.id = :postId")
	public List<Comment> findParentCommentsByPostId(@Param("postId") Integer postId);
}
