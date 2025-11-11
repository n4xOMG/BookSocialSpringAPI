package com.nix.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nix.exception.ResourceNotFoundException;
import com.nix.exception.SensitiveWordException;
import com.nix.models.Book;
import com.nix.models.Chapter;
import com.nix.models.Comment;
import com.nix.models.Post;
import com.nix.models.Report;
import com.nix.models.SensitiveWord;
import com.nix.models.User;
import com.nix.repository.BookRepository;
import com.nix.repository.ChapterRepository;
import com.nix.repository.CommentRepository;
import com.nix.repository.PostRepository;
import com.nix.repository.ReportRepository;
import com.nix.repository.SensitiveWordRepository;
import com.nix.repository.UserRepository;
import com.nix.service.CommentService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class CommentServiceImpl implements CommentService {

	private static final Logger logger = LoggerFactory.getLogger(CommentService.class);
	@Autowired
	CommentRepository commentRepo;

	@Autowired
	BookRepository bookRepo;

	@Autowired
	UserRepository userRepo;

	@Autowired
	ChapterRepository chapterRepo;

	@Autowired
	PostRepository postRepository;

	@Autowired
	SensitiveWordRepository sensitiveWordRepo;

	@Autowired
	ReportRepository reportRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public List<Comment> getAllComments() {
		return commentRepo.findAll();
	}

	@Override
	public List<Comment> getAllParentComments() {
		return commentRepo.findParentComments();
	}

	@Override
	public Page<Comment> getPagerBookComments(int page, int size, UUID bookId) {
		Pageable pageable = PageRequest.of(page, size);

		return commentRepo.findParentCommentsByBookId(bookId, pageable);
	}

	@Override
	public Page<Comment> getPagerChapterComments(int page, int size, UUID chapterId) {
		Pageable pageable = PageRequest.of(page, size);

		return commentRepo.findParentCommentsByChapterId(chapterId, pageable);
	}

	@Override
	public Page<Comment> getPagerPostComments(int page, int size, UUID postId) {
		Pageable pageable = PageRequest.of(page, size);

		return commentRepo.findParentCommentsByPostId(postId, pageable);

	}

	@Override
	public Page<Comment> getRecentCommentsByUserId(UUID userId, int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
		return commentRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable);
	}

	public boolean containsSensitiveWords(String content) {
		String normalizedContent = normalizeContent(content);

		List<SensitiveWord> sensitiveWords = sensitiveWordRepo.findAll();

		return sensitiveWords.stream().anyMatch(word -> {
			String normalizedWord = word.getWord().toLowerCase();

			// Use Pattern and Matcher to check for whole words, ignoring newlines and extra
			// spaces
			String wordPattern = "\\b" + Pattern.quote(normalizedWord) + "\\b";
			Pattern pattern = Pattern.compile(wordPattern, Pattern.DOTALL); // Use DOTALL to match across newlines
			Matcher matcher = pattern.matcher(normalizedContent);
			return matcher.find();
		});
	}

	// Helper method to normalize the content
	private String normalizeContent(String content) {
		String normalizedContent = normalizeLeetSpeak(content.toLowerCase());

		// Replace newlines, tabs, and multiple spaces with a single space
		normalizedContent = normalizedContent.replaceAll("[\\n\\r\\t]+", " ");
		normalizedContent = normalizedContent.replaceAll("\\s{2,}", " ");

		// Remove non-alphabetic characters (but retain spaces)
		return normalizedContent.replaceAll("[^a-z\\s]", "");
	}

	// Helper function to normalize leetspeak and symbol substitutions
	private String normalizeLeetSpeak(String content) {
		Map<Character, Character> leetSpeakMap = getLeetSpeakMap();

		// Replace leetspeak characters in the content
		StringBuilder normalizedContent = new StringBuilder();
		for (char c : content.toCharArray()) {
			// Replace character if it's in the leetspeak map, otherwise keep it as is
			normalizedContent.append(leetSpeakMap.getOrDefault(c, c));
		}
		return normalizedContent.toString();
	}

	// Define a leetspeak map for common letter substitutions
	private Map<Character, Character> getLeetSpeakMap() {
		Map<Character, Character> leetSpeakMap = new HashMap<>();
		leetSpeakMap.put('1', 'i');
		leetSpeakMap.put('!', 'i');
		leetSpeakMap.put('@', 'a');
		leetSpeakMap.put('3', 'e');
		leetSpeakMap.put('4', 'a');
		leetSpeakMap.put('0', 'o');
		leetSpeakMap.put('7', 't');
		leetSpeakMap.put('$', 's');
		return leetSpeakMap;
	}

	@Transactional
	public Comment createComment(Comment comment, UUID bookId, UUID chapterId, UUID postId, User user,
			boolean isBookComment, boolean isChapterComment, boolean isPostComment) throws Exception {

		if (containsSensitiveWords(comment.getContent())) {
			throw new SensitiveWordException("Comment contains sensitive words");
		}

		Comment newComment = new Comment();
		newComment.setUser(user);
		newComment.setContent(comment.getContent());
		newComment.setCreatedAt(LocalDateTime.now());

		if (isPostComment) {
			Post post = postRepository.findById(postId)
					.orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + postId));
			newComment.setPost(post);
			post.getComments().add(newComment);
		}

		if (isChapterComment) {
			Chapter chapter = chapterRepo.findById(chapterId)
					.orElseThrow(() -> new ResourceNotFoundException("Chapter not found with id " + chapterId));
			newComment.setChapter(chapter);

			Book chapterBook = chapter.getBook();
			if (chapterBook == null) {
				throw new Exception("Chapter with id " + chapterId + " is not associated with any book.");
			}
			newComment.setBook(chapterBook);
			chapterBook.getComments().add(newComment);
		}

		if (isBookComment) {
			Book book = bookRepo.findById(bookId)
					.orElseThrow(() -> new ResourceNotFoundException("Book not found with id " + bookId));
			newComment.setBook(book);
			book.getComments().add(newComment);
		}

		return commentRepo.save(newComment);
	}

	@Override
	@Transactional
	public Comment createBookComment(Comment comment, UUID bookId, User user) throws Exception {
		return createComment(comment, bookId, null, null, user, true, false, false);
	}

	@Override
	@Transactional
	public Comment createChapterComment(Comment comment, UUID chapterId, User user) throws Exception {
		return createComment(comment, null, chapterId, null, user, false, true, false);
	}

	@Override
	public Comment createPostComment(Comment comment, UUID postId, User user) throws Exception {
		return createComment(comment, null, null, postId, user, false, false, true);
	}

	@Override
	@Transactional
	public Boolean likeComment(UUID commentId, UUID userId) throws Exception {

		Optional<User> user = userRepo.findById(userId);
		if (user == null) {
			throw new Exception("User not found!");
		}

		Comment comment = findCommentById(commentId);
		if (comment == null) {
			throw new Exception("Comment not found!");
		}

		if (comment.getLikedUsers().contains(user.get())) {
			comment.getLikedUsers().remove(user.get());
			user.get().getLikedComments().remove(comment);
			return false;
		} else {
			comment.getLikedUsers().add(user.get());
			user.get().getLikedComments().add(comment);
			return true;
		}

	}

	@Override
	public Comment findCommentById(UUID commentId) {
		Comment comment = commentRepo.findById(commentId)
				.orElseThrow(() -> new ResourceNotFoundException("Comment not found with id " + commentId));
		return comment;

	}

	@Override
	@Transactional
	public String deleteComment(UUID commentId, UUID userId) throws Exception {
		Comment comment = findCommentById(commentId);
		User requestingUser = userRepo.findById(userId).orElseThrow(() -> new Exception("User not found"));

		// Authorization check: Only ADMIN or the comment owner can delete
		if (!requestingUser.getRole().getName().equals("ADMIN") && !comment.getUser().getId().equals(userId)) {
			throw new Exception("Cannot delete comment, invalid user!");
		}

		try {
			// Recursively delete all child comments first
			deleteCommentRecursively(comment);

			return "Comment deleted successfully!";
		} catch (Exception e) {
			logger.error("Error deleting comment {}", e);
			throw new Exception("Error deleting comment: " + e.getMessage(), e);
		}
	}

	private void deleteCommentRecursively(Comment comment) {
		// First, recursively delete all replies (depth-first)
		List<Comment> replies = new ArrayList<>(comment.getReplies());
		for (Comment reply : replies) {
			deleteCommentRecursively(reply);
		}

		// Remove associations in Reports to prevent FK constraints
		List<Report> reports = reportRepository.findByCommentId(comment.getId());
		for (Report report : reports) {
			report.setComment(null);
			reportRepository.save(report);
		}

		// Remove associations with liked users
		for (User user : new ArrayList<>(comment.getLikedUsers())) {
			user.getLikedComments().remove(comment);
		}
		comment.getLikedUsers().clear();

		// Remove the comment from its parent if it has one
		if (comment.getParentComment() != null) {
			Comment parent = comment.getParentComment();
			parent.getReplies().remove(comment);
			comment.setParentComment(null);
			entityManager.merge(parent);
		}

		// Now safe to delete this comment
		entityManager.remove(entityManager.contains(comment) ? comment : entityManager.merge(comment));
		entityManager.flush(); // Force immediate deletion
	}

	@Override
	public Comment editComment(UUID userId, UUID commentId, Comment comment) throws Exception {
		Comment editComment = findCommentById(commentId);

		if (editComment.getUser().getId() != userId) {
			throw new Exception("Cannot edit comment, invaid user!");
		}

		if (comment.getContent() != null) {
			editComment.setContent(comment.getContent());
			editComment.setCreatedAt(LocalDateTime.now());
		}

		return commentRepo.save(editComment);
	}

	@Transactional
	public Comment createReplyComment(Comment comment, UUID parentCommentId, User user, boolean isBookComment,
			boolean isChapterComment, boolean isPostComment) throws Exception {
		Comment parentComment = findCommentById(parentCommentId);

		if (parentComment == null) {
			throw new ResourceNotFoundException("Parent comment not found!");
		}

		if (containsSensitiveWords(comment.getContent())) {
			throw new SensitiveWordException("Comment contains sensitive words");
		}

		Comment newComment = new Comment();
		newComment.setUser(user);
		newComment.setContent(comment.getContent());
		newComment.setCreatedAt(LocalDateTime.now());
		newComment.setParentComment(parentComment);
		if (isBookComment) {
			newComment.setBook(parentComment.getBook());
		}

		if (isChapterComment) {
			newComment.setBook(parentComment.getBook());
			newComment.setChapter(parentComment.getChapter());
		}

		if (isPostComment) {
			newComment.setPost(parentComment.getPost());
		}

		parentComment.getReplies().add(newComment);

		return commentRepo.save(newComment);
	}

	@Override
	@Transactional
	public Comment createReplyBookComment(Comment comment, UUID parentCommentId, User user) throws Exception {
		return createReplyComment(comment, parentCommentId, user, true, false, false);
	}

	@Override
	@Transactional
	public Comment createReplyChapterComment(Comment comment, UUID parentCommentId, User user) throws Exception {
		return createReplyComment(comment, parentCommentId, user, false, true, false);
	}

	@Override
	@Transactional
	public Comment createReplyPostComment(Comment comment, UUID parentCommentId, User user) throws Exception {
		return createReplyComment(comment, parentCommentId, user, false, false, true);
	}

	@Override
	public Boolean isCommentLikedByCurrentUser(UUID commentId, User user) {
		Optional<Comment> commentOpt = commentRepo.findById(commentId);
		if (commentOpt.isPresent()) {
			return user.getLikedComments().contains(commentOpt.get());
		} else {
			return false;
		}
	}

}
