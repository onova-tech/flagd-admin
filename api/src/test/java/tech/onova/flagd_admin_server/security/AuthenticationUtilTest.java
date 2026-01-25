package tech.onova.flagd_admin_server.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import tech.onova.flagd_admin_server.domain.exception.AuthenticationException;

import static org.assertj.core.api.Assertions.*;

class AuthenticationUtilTest {

    @Test
    void getCurrentUsername_WhenNotAuthenticated_ShouldThrowException() {
        // Given
        SecurityContextHolder.clearContext();

        // When & Then
        assertThatThrownBy(() -> AuthenticationUtil.getCurrentUsername())
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("User not authenticated");
    }

    @Test
    void getCurrentUsernameSafely_WhenNotAuthenticated_ShouldReturnEmpty() {
        // Given
        SecurityContextHolder.clearContext();

        // When
        var result = AuthenticationUtil.getCurrentUsernameSafely();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getCurrentUsername_WhenAuthenticated_ShouldReturnUsername() {
        // Given
        SecurityContextHolder.clearContext();
        org.springframework.security.core.userdetails.UserDetails user = User.withUsername("testUser").password("password").roles("USER").build();
        Authentication authentication = org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                .authenticated(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // When
        var username = AuthenticationUtil.getCurrentUsername();

        // Then
        assertThat(username).isEqualTo("testUser");

        // Cleanup
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUsernameSafely_WhenAuthenticated_ShouldReturnUsername() {
        // Given
        SecurityContextHolder.clearContext();
        org.springframework.security.core.userdetails.UserDetails user = User.withUsername("testUser").password("password").roles("USER").build();
        Authentication authentication = org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                .authenticated(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // When
        var result = AuthenticationUtil.getCurrentUsernameSafely();

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("testUser");

        // Cleanup
        SecurityContextHolder.clearContext();
    }
}