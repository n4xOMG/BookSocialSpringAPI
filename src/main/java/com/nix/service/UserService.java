package com.nix.service;

import java.io.UnsupportedEncodingException;

import org.springframework.data.domain.Page;

import com.nix.models.User;

import jakarta.mail.MessagingException;

public interface UserService {

	public Page<User> getAllUsers(int page, int size, String searchTerm);

	public User register(User user, String url) throws UnsupportedEncodingException, MessagingException;

	public User sendForgotPasswordMail(User user, String url) throws UnsupportedEncodingException, MessagingException;

	public User findUserById(Integer userId) throws Exception;

	public User findUserByEmail(String email);

	public User findUserByJwt(String jwt);

	public User updateCurrentSessionUser(String jwt, User user, String url) throws UnsupportedEncodingException, MessagingException;

	public User updateUser(Integer userId, User user) throws Exception;

	public String deleteUser(Integer userId) throws Exception;

	public User updateUserPassword(String password, User user);

	public User verifyUser(String verificationCode);

	public User suspendUser(Integer userId) throws Exception;

	public User unsuspendUser(Integer userId) throws Exception;
}
