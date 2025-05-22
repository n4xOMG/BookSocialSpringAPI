package com.nix.service;

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.springframework.data.domain.Page;

import com.nix.dtos.UserDTO;
import com.nix.models.User;

import jakarta.mail.MessagingException;

public interface UserService {

	public Page<User> getAllUsers(int page, int size, String searchTerm);

	public Long getUserCount();

	public User register(User user) throws UnsupportedEncodingException, MessagingException;

	public User sendForgotPasswordMail(User user) throws UnsupportedEncodingException, MessagingException;

	public User findUserById(Long userId);

	public User findUserByEmail(String email);

	public User findUserByJwt(String jwt);

	public List<User> findUserByUsername(String username);

	public User updateCurrentSessionUser(String jwt, User user, String url)
			throws UnsupportedEncodingException, MessagingException;

	public void updateUserLoginAttemptsNumber(String email);

	public void resetLoginAttempts(String email);

	public void updateUserLastLoginDate(String email);

	public User updateUserRole(Long userId, String roleName);

	public User updateUser(Long userId, User user);

	public String deleteUser(Long userId);

	public User updateUserPassword(String password, User user);

	public User verifyUser(String verificationCode);

	public User suspendUser(Long userId);

	public User unsuspendUser(Long userId);

	public User banUser(Long userId, String banReason);

	public User unbanUser(Long userId);

	public User followUser(Long currentUserId, Long followedUserId);

	public User unFollowUser(Long currentUserId, Long unfollowedUserId);

	public boolean isFollowedByCurrentUser(User currentUser, User otherUser);

	public UserDTO getUserPreferences(Long userId);

	public List<User> getUserFollowers(Long userId);

	public List<User> getUserFollowing(Long userId);

	public List<Long> getNewUsersByMonth();

	public long getTotalUsers();

	public long getBannedUsersCount();

	public long getSuspendedUsersCount();

}
