package tech.onova.flagd_admin_server.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import tech.onova.flagd_admin_server.domain.exception.AuthenticationException;

import java.util.Optional;

public class AuthenticationUtil {

    /**
     * Gets the current authenticated username.
     * 
     * @return the username of the authenticated user
     * @throws AuthenticationException if no user is authenticated
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || 
            authentication.getPrincipal() instanceof String && "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AuthenticationException("User not authenticated");
        }
        
        if (authentication.getPrincipal() instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        
        return authentication.getName();
    }

    /**
     * Gets the current authenticated username safely.
     * 
     * @return Optional containing the username if authenticated, empty otherwise
     */
    public static Optional<String> getCurrentUsernameSafely() {
        try {
            return Optional.of(getCurrentUsername());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}