package com.nix.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.nix.models.User;
import com.nix.repository.UserRepository;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtValidator extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtValidator.class);

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    public JwtValidator(UserRepository userRepository, JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String jwt = request.getHeader(JwtConstant.JWT_HEADER);
        if (jwt != null) {
            try {
                String email = jwtProvider.getEmailFromJwtToken(jwt);

                // Fetch user from database to get current role and status
                User user = userRepository.findByEmail(email);
                if (user == null) {
                    logger.warn("User not found for email: {}", email);
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not found");
                    return;
                }

                // Check if user is banned - banned users cannot access the site at all
                if (user.isBanned()) {
                    logger.warn("Banned user attempted access: {}", email);
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Account is banned");
                    return;
                }

                // Note: Suspended users CAN access the API (browse, read content)
                // They are only blocked from content creation activities by controller-level
                // checks

                // Use role from database, not from JWT - this prevents privilege escalation
                String roleName = user.getRole() != null ? user.getRole().getName() : "USER";

                logger.debug("User with email: {} has role: {} (from database)", email, roleName);

                // Build authorities from database role
                Collection<GrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));

                Authentication authentication = new UsernamePasswordAuthenticationToken(email, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (MalformedJwtException e) {
                logger.error("Invalid token format: {}", e.getMessage());
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid token format");
                return;
            } catch (ExpiredJwtException e) {
                logger.warn("Token has expired: {}", e.getMessage());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token has expired");
                return;
            } catch (JwtException e) {
                logger.error("Invalid token: {}", e.getMessage());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
                return;
            } catch (Exception e) {
                logger.error("Error processing token", e);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error processing token");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
