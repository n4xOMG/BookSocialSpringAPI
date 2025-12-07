package com.nix.util;

import com.nix.models.User;

/**
 * Utility class for security-related checks.
 * Centralizes role checking logic to avoid scattered string comparisons.
 */
public final class SecurityUtils {

    // Role constants
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_MODERATOR = "MODERATOR";
    public static final String ROLE_AUTHOR = "AUTHOR";
    public static final String ROLE_USER = "USER";

    private SecurityUtils() {
        // Prevent instantiation
    }

    /**
     * Check if the user has the ADMIN role.
     */
    public static boolean isAdmin(User user) {
        return hasRole(user, ROLE_ADMIN);
    }

    /**
     * Check if the user has the MODERATOR role or higher (ADMIN).
     */
    public static boolean isModeratorOrHigher(User user) {
        return isAdmin(user) || hasRole(user, ROLE_MODERATOR);
    }

    /**
     * Check if the user has the AUTHOR role.
     */
    public static boolean isAuthor(User user) {
        return hasRole(user, ROLE_AUTHOR);
    }

    /**
     * Check if the user has any of the specified roles.
     */
    public static boolean hasAnyRole(User user, String... roleNames) {
        if (user == null || user.getRole() == null || roleNames == null) {
            return false;
        }
        String userRole = user.getRole().getName();
        for (String roleName : roleNames) {
            if (roleName != null && roleName.equalsIgnoreCase(userRole)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the user has the specified role.
     */
    public static boolean hasRole(User user, String roleName) {
        if (user == null || user.getRole() == null || roleName == null) {
            return false;
        }
        return roleName.equalsIgnoreCase(user.getRole().getName());
    }
}
