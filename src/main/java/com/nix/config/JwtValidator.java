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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String jwt = request.getHeader(JwtConstant.JWT_HEADER);
        if (jwt != null) {
            try {
                String email = JwtProvider.getEmailFromJwtToken(jwt);
                String roleFromToken = JwtProvider.getRoleFromJwtToken(jwt);

                // Extract role name if it starts with "ROLE_" prefix, otherwise use as-is
                String roleName = roleFromToken.startsWith("ROLE_")
                        ? roleFromToken.substring(5)
                        : roleFromToken;

                logger.debug("User with email: {} has role: {}", email, roleName);

                // Build authorities directly from JWT without database call
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
