package com.nix.service;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.UUID;

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
import com.nix.exception.ResourceNotFoundException;
import com.nix.models.Comment;
import com.nix.models.Role;
import com.nix.models.User;
import com.nix.repository.CommentRepository;
import com.nix.repository.RoleRepository;
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
	RoleRepository roleRepo;

	@Autowired
	CommentRepository commentRepo;

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
	public User register(User user, String url) throws UnsupportedEncodingException, MessagingException {
		User newUser = new User();
		newUser.setEmail(user.getEmail());
		newUser.setUsername(user.getUsername());
		newUser.setFullname(user.getFullname());
		newUser.setGender(user.getGender());
		newUser.setAvatarUrl(user.getAvatarUrl());
		newUser.setIsVerified(false);
		newUser.setBanned(false);
		newUser.setBirthdate(user.getBirthdate());
		newUser.setBio(null);
		newUser.setBanReason(null);
		newUser.setPassword(passEncoder.encode(user.getPassword()));

		String randomCode = generateRandomCode();

		newUser.setVerificationCode(randomCode);

		newUser.setRole(roleRepo.findByName("USER"));

		User savedUser = userRepo.save(newUser);
		sendMail(savedUser, url, "Welcome to our fandom!",
				"Dear [[username]],<br>" + "Please click the link below to complete signing up:<br>"
						+ "<h3><a href=\"[[URL]]\" target=\"_self\">SIGN UP</a></h3>" + "Thank you,<br>" + "nixOMG.",
				"register");
		;
		return savedUser;
	}

	@Override
	public User sendForgotPasswordMail(User user, String url) throws UnsupportedEncodingException, MessagingException {
		user.setIsVerified(false);

		String randomCode = generateRandomCode();

		user.setVerificationCode(randomCode);

		User reqUser = userRepo.save(user);
		sendMail(reqUser, url, "Confirm changing password.",
				"Dear [[username]],<br>" + "Please click the link below to confirm changing your password:<br>"
						+ "<h3><a href=\"[[URL]]\" target=\"_self\">VERIFY</a></h3>" + "Thank you,<br>" + "nixOMG.",
				"reset-password");

		return reqUser;
	}

	@Override
	public User findUserById(Integer userId) {
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

		if (user.getEmail() != null && !user.getEmail().equals(userUpdate.getEmail())) {
			user.setVerificationCode(generateRandomCode());
			userUpdate.setVerificationCode(user.getVerificationCode());
			userRepo.save(userUpdate);
			sendMail(user, url, "Email Verification", "Please verify your email by clicking the link: [[URL]]",
					"updateProfile");
		}
		if (user.getUsername() != null) {
			userUpdate.setUsername(user.getUsername());
		}
		if (user.getFullname() != null) {
			userUpdate.setUsername(user.getUsername());
		}
		if (user.getGender() != null) {
			userUpdate.setGender(user.getGender());
		}
		if (user.getBirthdate() != null) {
			userUpdate.setBirthdate(user.getBirthdate());
		}
		if (user.getAvatarUrl() != null) {
			userUpdate.setAvatarUrl(user.getAvatarUrl());
		}
		if (user.getBio() != null) {
			userUpdate.setBio(user.getBio());
		}
		if (user.getIsVerified() != null) {
			userUpdate.setIsVerified(user.getIsVerified());
		}
		return userRepo.save(userUpdate);
	}

	@Override
	@Transactional
	public String deleteUser(Integer userId) {
		User user = findUserById(userId);
		try {
			if (user != null) {

				List<Comment> comments = commentRepo.findByUserId(userId);
				commentRepo.deleteAll(comments);

				user.getRole().getUsers().remove(user);
				user.getFollowedBooks().forEach(book -> book.getFavoured().remove(user));
				user.getLikedComments().forEach(comment -> comment.getLikedUsers().remove(user));

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

	private void sendMail(User user, String siteURL, String subject, String content, String action)
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
		String mailVerifyUrl = siteURL + "/auth/verify?code=" + user.getVerificationCode() + "&action=" + action
				+ "&email=" + user.getEmail();

		content = content.replace("[[URL]]", mailVerifyUrl);

		helper.setText(content, true);
		System.out.println("Mail sent to: " + user.getEmail());
		mailSender.send(message);
	}

	public String generateRandomCode() {
		String uuid = UUID.randomUUID().toString().replace("-", "");
		StringBuilder randomCode = new StringBuilder();
		for (int i = 0; i < 2; i++) {
			randomCode.append(uuid);
		}
		return randomCode.toString();
	}

	@Override
	public User updateUserPassword(String newPassword, User user) {
		user.setPassword(passEncoder.encode(newPassword));
		return userRepo.save(user);
	}

	@Override
	public User suspendUser(Integer userId) {
		User user = findUserById(userId);
		user.setIsSuspended(true);

		return userRepo.save(user);
	}

	@Override
	public User unsuspendUser(Integer userId) {
		User user = findUserById(userId);
		user.setIsSuspended(false);

		return userRepo.save(user);
	}
	@Override
	public User banUser(Integer userId) {
		User user = findUserById(userId);
		user.setBanned(true);

		return userRepo.save(user);
	}
	@Override
	public User unbanUser(Integer userId) {
		User user = findUserById(userId);
		user.setBanned(true);

		return userRepo.save(user);
	}
	@Override
	public User updateUser(Integer userId, User user) {

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
		
		if (user.getAvatarUrl()!=null) {
			userUpdate.setAvatarUrl(user.getAvatarUrl());
		}

		return userRepo.save(userUpdate);
	}
	@Override
	@Transactional
    public User updateUserRole(Integer userId, String roleName){
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        Role newRole = roleRepo.findByName(roleName);
        		if(newRole==null) {
        			throw new ResourceNotFoundException("Role not found with: "+roleName);
        		}

        user.setRole(newRole);
        return userRepo.save(user);
    }

	@Override
	public List<User> findUserByUsername(String username) {
		return userRepo.findByUsername(username);
	}
}
