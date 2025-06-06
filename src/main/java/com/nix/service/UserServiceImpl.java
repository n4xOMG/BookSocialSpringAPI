package com.nix.service;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nix.config.JwtProvider;
import com.nix.dtos.CategoryDTO;
import com.nix.dtos.TagDTO;
import com.nix.dtos.UserDTO;
import com.nix.dtos.mappers.BookMapper;
import com.nix.exception.ResourceNotFoundException;
import com.nix.models.Book;
import com.nix.models.Category;
import com.nix.models.Comment;
import com.nix.models.ReadingProgress;
import com.nix.models.Role;
import com.nix.models.Tag;
import com.nix.models.User;
import com.nix.models.UserFollow;
import com.nix.repository.BookRepository;
import com.nix.repository.CommentRepository;
import com.nix.repository.RatingRepository;
import com.nix.repository.ReadingProgressRepository;
import com.nix.repository.RoleRepository;
import com.nix.repository.UserFollowRepository;
import com.nix.repository.UserRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class UserServiceImpl implements UserService {
	@Autowired
	private PasswordEncoder passEncoder;

	@Autowired
	private JavaMailSender mailSender;

	@Autowired
	UserRepository userRepo;

	@Autowired
	UserFollowRepository userFollowRepository;

	@Autowired
	RoleRepository roleRepo;

	@Autowired
	CommentRepository commentRepo;

	@Autowired
	ReadingProgressRepository readingProgressRepository;

	@Autowired
	RatingRepository ratingRepository;

	@Autowired
	BookRepository bookRepository;

	@Autowired
	BookMapper bookMapper;

	@Override
	public Page<User> getAllUsers(int page, int size, String searchTerm) {
		Pageable pageable = PageRequest.of(page, size);
		if (searchTerm == null || searchTerm.isEmpty()) {
			return userRepo.findAll(pageable);
		} else {
			return userRepo.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(searchTerm, searchTerm,
					pageable);
		}
	}

	@Override
	public User register(User user) throws UnsupportedEncodingException, MessagingException {
		User newUser = new User();
		newUser.setEmail(user.getEmail());
		newUser.setUsername(user.getUsername());
		newUser.setFullname(user.getFullname());
		newUser.setGender(user.getGender());
		newUser.setAvatarUrl(user.getAvatarUrl());
		newUser.setIsVerified(false);
		newUser.setIsSuspended(false);
		newUser.setBanned(false);
		newUser.setBirthdate(user.getBirthdate());
		newUser.setBio(user.getBio());
		newUser.setPassword(passEncoder.encode(user.getPassword()));
		newUser.setAccountCreatedDate(LocalDateTime.now());
		newUser.setLastLoginDate(LocalDateTime.now());
		newUser.setLoginAttempts(0);

		String randomCode = generateRandomCode();

		newUser.setVerificationCode(randomCode);

		newUser.setRole(roleRepo.findByName("USER"));

		User savedUser = userRepo.save(newUser);
		sendMail(savedUser, "Welcome to our fandom!", "Dear [[username]],<br>"
				+ "Your OTP code to complete signing up is: <b>[[OTP]]</b><br>" + "Thank you,<br>" + "nixOMG.");
		;
		return savedUser;
	}

	@Override
	public User sendForgotPasswordMail(User user) throws UnsupportedEncodingException, MessagingException {
		user.setIsVerified(false);

		String randomCode = generateRandomCode();

		user.setVerificationCode(randomCode);

		User reqUser = userRepo.save(user);
		sendMail(reqUser, "Your OTP Code", "Dear [[username]],<br>"
				+ "Your OTP code for password reset is: <b>[[OTP]]</b><br>" + "Thank you,<br>" + "nixOMG.");

		return reqUser;
	}

	@Override
	public User findUserById(Long userId) {
		return userRepo.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

	}

	@Override
	public User findUserByEmail(String email) {
		User user = userRepo.findByEmail(email);
		if (user != null) {
			return user;
		}
		return null;
	}

	@Override
	public User findUserByJwt(String jwt) {
		String email = JwtProvider.getEmailFromJwtToken(jwt);
		return userRepo.findByEmail(email);
	}

	@Override
	public User updateCurrentSessionUser(String jwt, User user, String url)
			throws UnsupportedEncodingException, MessagingException {

		User userUpdate = findUserByJwt(jwt);

		// Generate OTP if email is changed
		if (user.getEmail() != null && !user.getEmail().equals(userUpdate.getEmail())) {
			String otpCode = generateRandomCode();
			user.setVerificationCode(otpCode);
			user.setIsVerified(false);
			userUpdate.setVerificationCode(otpCode);
			userUpdate.setIsVerified(false); // Mark email as unverified until OTP is confirmed
			userRepo.save(userUpdate);

			sendMail(user, "Email Verification", "Please verify your email with this OTP: " + otpCode);
			return userUpdate; // Return user without updating email yet
		}

		// Other profile updates (allowed directly)
		if (user.getUsername() != null)
			userUpdate.setUsername(user.getUsername());
		if (user.getFullname() != null)
			userUpdate.setFullname(user.getFullname());
		if (user.getGender() != null)
			userUpdate.setGender(user.getGender());
		if (user.getBirthdate() != null)
			userUpdate.setBirthdate(user.getBirthdate());
		if (user.getAvatarUrl() != null)
			userUpdate.setAvatarUrl(user.getAvatarUrl());
		if (user.getBio() != null)
			userUpdate.setBio(user.getBio());

		return userRepo.save(userUpdate);
	}

	@Override
	@Transactional
	public String deleteUser(Long userId) {
		User user = findUserById(userId);
		try {
			if (user != null) {

				List<Comment> comments = commentRepo.findByUserId(userId);
				commentRepo.deleteAll(comments);

				user.getRole().getUsers().remove(user);
				user.getFollowedBooks().forEach(book -> book.getFavoured().remove(user));
				user.getLikedComments().forEach(comment -> comment.getLikedUsers().remove(user));
				user.getLikedPosts().forEach(post -> post.getLikedUsers().remove(user));
				userRepo.delete(user);

			}
			return "User deleted successfully!";
		} catch (Exception e) {
			return "Error deleting user: " + e;
		}

	}

	public User verifyUser(String code) {
		User user = userRepo.findByVerificationCode(code);
		if (user == null) {
			return null;
		}
		user.setIsVerified(true);
		userRepo.save(user);
		return user;
	}

	private void sendMail(User user, String subject, String content)
			throws MessagingException, UnsupportedEncodingException {
		String toAddress = user.getEmail();
		String fromAddress = "testnixomg123@gmail.com";
		String senderName = "nixOMG";

		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);

		helper.setFrom(fromAddress, senderName);
		helper.setTo(toAddress);
		helper.setSubject(subject);

		if (user.getUsername() != null) {
			content = content.replace("[[username]]", user.getUsername());
		} else {
			content = content.replace("[[username]]", "User");
		}

		content = content.replace("[[OTP]]", user.getVerificationCode());

		helper.setText(content, true);
		System.out.println("Mail sent to: " + user.getEmail());
		mailSender.send(message);
	}

	public String generateRandomCode() {
		Random random = new Random();
		int otp = 100000 + random.nextInt(900000); // Generate a random 6-digit OTP
		return String.valueOf(otp);
	}

	@Override
	public User updateUserPassword(String newPassword, User user) {
		user.setPassword(passEncoder.encode(newPassword));
		return userRepo.save(user);
	}

	@Override
	public User suspendUser(Long userId) {
		User user = findUserById(userId);
		user.setIsSuspended(true);

		return userRepo.save(user);
	}

	@Override
	public User unsuspendUser(Long userId) {
		User user = findUserById(userId);
		user.setIsSuspended(false);

		return userRepo.save(user);
	}

	@Override
	public User banUser(Long userId, String banReason) {
		User user = findUserById(userId);
		user.setBanned(true);
		user.setBanReason(banReason);
		user.setBanDate(LocalDateTime.now());

		return userRepo.save(user);
	}

	@Override
	public User unbanUser(Long userId) {
		User user = findUserById(userId);
		user.setBanned(false);
		user.setBanReason(null);
		user.setBanDate(null);

		return userRepo.save(user);
	}

	@Override
	public User updateUser(Long userId, User user) {

		User userUpdate = findUserById(userId);

		if (user.getEmail() != null) {
			userUpdate.setEmail(user.getEmail());
		}
		if (user.getUsername() != null) {
			userUpdate.setUsername(user.getUsername());
		}
		if (user.getIsVerified() != null) {
			userUpdate.setIsVerified(user.getIsVerified());
		}

		if (user.getAvatarUrl() != null) {
			userUpdate.setAvatarUrl(user.getAvatarUrl());
		}

		return userRepo.save(userUpdate);
	}

	@Override
	@Transactional
	public User updateUserRole(Long userId, String roleName) {
		User user = userRepo.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

		Role newRole = roleRepo.findByName(roleName);
		if (newRole == null) {
			throw new ResourceNotFoundException("Role not found with: " + roleName);
		}

		user.setRole(newRole);
		return userRepo.save(user);
	}

	@Override
	public List<User> findUserByUsername(String username) {
		return userRepo.findByUsername(username);
	}

	@Override
	@Transactional
	public User followUser(Long currentUserId, Long followedUserId) {
		if (currentUserId.equals(followedUserId)) {
			throw new IllegalArgumentException("Users cannot follow themselves.");
		}

		User currentUser = userRepo.findById(currentUserId)
				.orElseThrow(() -> new ResourceNotFoundException("Current user not found."));
		User followedUser = userRepo.findById(followedUserId)
				.orElseThrow(() -> new ResourceNotFoundException("User to follow not found."));

		// Check if already following
		boolean isAlreadyFollowing = userFollowRepository.findByFollowerAndFollowed(currentUser, followedUser)
				.isPresent();
		if (isAlreadyFollowing) {
			throw new IllegalStateException("Already following this user.");
		}

		// Create new UserFollow
		UserFollow userFollow = new UserFollow();
		userFollow.setFollower(currentUser);
		userFollow.setFollowed(followedUser);
		userFollow.setFollowDate(LocalDateTime.now());

		// Save the follow relationship
		userFollowRepository.save(userFollow);

		currentUser.getFollowing().add(userFollow);
		followedUser.getFollowers().add(userFollow);

		return userRepo.save(currentUser);
	}

	@Override
	@Transactional
	public User unFollowUser(Long currentUserId, Long unfollowedUserId) {
		if (currentUserId.equals(unfollowedUserId)) {
			throw new IllegalArgumentException("Users cannot unfollow themselves.");
		}

		User currentUser = userRepo.findById(currentUserId)
				.orElseThrow(() -> new ResourceNotFoundException("Current user not found."));
		User unfollowedUser = userRepo.findById(unfollowedUserId)
				.orElseThrow(() -> new ResourceNotFoundException("Followed user not found."));

		// Find the existing follow relationship
		UserFollow userFollow = userFollowRepository.findByFollowerAndFollowed(currentUser, unfollowedUser)
				.orElseThrow(() -> new IllegalStateException("Not following this user."));

		// Remove the follow relationship
		userFollowRepository.delete(userFollow);

		// Optionally, update the followers and following lists
		currentUser.getFollowing().remove(userFollow);
		unfollowedUser.getFollowers().remove(userFollow);

		return userRepo.save(currentUser);
	}

	@Override
	public boolean isFollowedByCurrentUser(User currentUser, User otherUser) {
		Optional<UserFollow> user = userFollowRepository.findByFollowerAndFollowed(currentUser, otherUser);
		return user.isPresent();
	}

	@Override
	public UserDTO getUserPreferences(Long userId) {
		User user = userRepo.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

		// Fetch reading progresses
		List<ReadingProgress> progresses = readingProgressRepository.findByUserId(userId);

		// Map to store total progress per book
		Map<Long, Double> bookProgressMap = new HashMap<>();

		for (ReadingProgress progress : progresses) {
			Long bookId = progress.getChapter().getBook().getId();
			Double chapterProgress = progress.getProgress(); // Assuming 0.0 to 100.0

			// Normalize chapter progress to 0.0 - 1.0
			Double normalizedProgress = chapterProgress / 100.0;

			// Sum normalized progress per book
			bookProgressMap.merge(bookId, normalizedProgress, Double::sum);
		}

		// Fetch books based on aggregated progress
		Set<Long> bookIds = bookProgressMap.keySet();
		List<Book> books = bookRepository.findAllById(bookIds);

		// Calculate preferred categories
		Map<Category, Double> categoryWeight = new HashMap<>();
		for (Book book : books) {
			Double progress = bookProgressMap.getOrDefault(book.getId(), 0.0);
			// Weight can be adjusted based on requirements
			// For example, progress could be multiplied by a factor
			Double weight = progress;

			categoryWeight.merge(book.getCategory(), weight, Double::sum);
		}

		List<CategoryDTO> preferredCategories = categoryWeight.entrySet().stream()
				.sorted(Map.Entry.<Category, Double>comparingByValue().reversed()).limit(5) // Top 5 categories
				.map(entry -> new CategoryDTO(entry.getKey().getId(), entry.getKey().getName(),
						entry.getKey().getDescription(), bookMapper.mapToDTOs(entry.getKey().getBooks())))
				.collect(Collectors.toList());

		// Calculate preferred tags
		Map<Tag, Double> tagWeight = new HashMap<>();
		for (Book book : books) {
			Double progress = bookProgressMap.getOrDefault(book.getId(), 0.0);
			Double weight = progress;

			for (Tag tag : book.getTags()) {
				tagWeight.merge(tag, weight, Double::sum);
			}
		}

		List<TagDTO> preferredTags = tagWeight.entrySet().stream()
				.sorted(Map.Entry.<Tag, Double>comparingByValue().reversed()).limit(10) // Top 10 tags
				.map(entry -> new TagDTO(entry.getKey().getId(), entry.getKey().getName()))
				.collect(Collectors.toList());
		// Map to UserDTO
		UserDTO userDTO = new UserDTO();
		userDTO.setId(user.getId());
		userDTO.setFullname(user.getFullname());
		// ... set other User fields as needed

		userDTO.setPreferredCategories(preferredCategories);
		userDTO.setPreferredTags(preferredTags);

		return userDTO;
	}

	@Override
	@Transactional(readOnly = true)
	public List<User> getUserFollowers(Long userId) throws ResourceNotFoundException {

		List<UserFollow> followerRelations = userFollowRepository.findByFollowedId(userId);

		List<User> followers = followerRelations.stream().map(UserFollow::getFollower).collect(Collectors.toList());

		return followers;
	}

	@Override
	@Transactional(readOnly = true)
	public List<User> getUserFollowing(Long userId) throws ResourceNotFoundException {

		List<UserFollow> followingRelations = userFollowRepository.findByFollowerId(userId);

		List<User> following = followingRelations.stream().map(UserFollow::getFollowed).collect(Collectors.toList());

		return following;
	}

	@Override
	public Long getUserCount() {
		return userRepo.count();
	}

	@Override
	public void updateUserLoginAttemptsNumber(String email) {
		User user = userRepo.findByEmail(email);
		user.setLoginAttempts(user.getLoginAttempts() + 1);

		userRepo.save(user);
	}

	@Override
	public void updateUserLastLoginDate(String email) {
		User user = userRepo.findByEmail(email);
		user.setLastLoginDate(LocalDateTime.now());

		userRepo.save(user);
	}

	@Override
	public void resetLoginAttempts(String email) {
		User user = userRepo.findByEmail(email);
		user.setLoginAttempts(0);

		userRepo.save(user);

	}

	@Override
	public List<Long> getNewUsersByMonth() {
		return userRepo.countNewUsersByMonth();
	}

	@Override
	public long getTotalUsers() {
		return userRepo.count();
	}

	@Override
	public long getBannedUsersCount() {
		return userRepo.countBannedUsers();
	}

	@Override
	public long getSuspendedUsersCount() {
		return userRepo.countSuspendedUsers();
	}
}
