package com.nix.service;

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.springframework.data.domain.Page;

import com.nix.dtos.UserDTO;
import com.nix.models.User;

import jakarta.mail.MessagingException;

public interface UserService {

	public Page<User> getAllUsers(int page, int size, String searchTerm);

	public User register(User user, String url) throws UnsupportedEncodingException, MessagingException;

	public User sendForgotPasswordMail(User user, String url) throws UnsupportedEncodingException, MessagingException;

	public User findUserById(Integer userId);

	public User findUserByEmail(String email);

	public User findUserByJwt(String jwt);

	public List<User> findUserByUsername(String username);

	public User updateCurrentSessionUser(String jwt, User user, String url)
			throws UnsupportedEncodingException, MessagingException;

	public User updateUserRole(Integer userId, String roleName);

	public User updateUser(Integer userId, User user);

	public String deleteUser(Integer userId);

	public User updateUserPassword(String password, User user);

	public User verifyUser(String verificationCode);

	public User suspendUser(Integer userId);

	public User unsuspendUser(Integer userId);

	public User banUser(Integer userId);

	public User unbanUser(Integer userId);

	public User followUser(Integer currentUserId, Integer followedUserId);

	public User unFollowUser(Integer currentUserId, Integer unfollowedUserId);
	
	public boolean isFollowedByCurrentUser(User currentUser, User otherUser);
	
	public UserDTO getUserPreferences(Integer userId);
}
