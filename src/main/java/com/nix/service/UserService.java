package com.nix.service;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;

import com.nix.dtos.UserDTO;
import com.nix.models.User;

import jakarta.mail.MessagingException;

public interface UserService {

	public Page<User> getAllUsers(int page, int size, String searchTerm);

	public Long getUserCount();

	public User register(User user) throws UnsupportedEncodingException, MessagingException;

	public User sendForgotPasswordMail(User user) throws UnsupportedEncodingException, MessagingException;

	public User findUserById(UUID userId);

	public User findUserByEmail(String email);

	public User findUserByJwt(String jwt);

	public List<User> findUserByUsername(String username);

	public User updateCurrentSessionUser(String jwt, User user, String url)
			throws UnsupportedEncodingException, MessagingException;

	public void updateUserLoginAttemptsNumber(String email);

	public void resetLoginAttempts(String email);

	public void updateUserLastLoginDate(String email);

	public User updateUserRole(UUID userId, String roleName);

	public User updateUser(UUID userId, User user);

	public User adminUpdateUser(UUID userId, com.nix.dtos.AdminUpdateUserDTO userDTO);

	public List<User> banUsers(List<UUID> userIds, String banReason);

	public String deleteUser(UUID userId);

	public User updateUserPassword(String password, User user);

	// Password reset token methods
	public String generatePasswordResetToken(User user);

	public boolean validatePasswordResetToken(User user, String token);

	public User resetPasswordWithToken(String newPassword, User user);

	public User verifyUser(String verificationCode);

	// Email change confirmation and rollback methods
	public User confirmEmailChange(String jwt, String otp) throws UnsupportedEncodingException, MessagingException;

	public String generateEmailRecoveryToken(User user);

	public User rollbackEmail(String token);

	public User suspendUser(UUID userId);

	public User unsuspendUser(UUID userId);

	public User banUser(UUID userId, String banReason);

	public User unbanUser(UUID userId);

	public User followUser(UUID currentUserId, UUID followedUserId);

	public User unFollowUser(UUID currentUserId, UUID unfollowedUserId);

	public boolean isFollowedByCurrentUser(User currentUser, User otherUser);

	public UserDTO getUserPreferences(UUID userId);

	public List<User> getUserFollowers(UUID userId);

	public List<User> getUserFollowing(UUID userId);

	public User blockUser(UUID blockerId, UUID blockedUserId);

	public void unblockUser(UUID blockerId, UUID blockedUserId);

	public boolean isBlockedBy(UUID viewerId, UUID potentialBlockerId);

	public boolean hasBlocked(UUID blockerId, UUID blockedUserId);

	public List<User> getBlockedUsers(UUID blockerId);

	public Set<UUID> getUserIdsBlocking(UUID blockedUserId);

	public Set<UUID> getBlockedUserIds(UUID blockerId);

	public Long getTotalUsers();

	public Long getBannedUsersCount();

	public Long getSuspendedUsersCount();

}
