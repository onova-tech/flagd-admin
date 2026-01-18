package tech.onova.flagd_admin_server.security.providers;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class NoAuthProviderTest {

    @Test
    void shouldImplementAuthProvider() {
        // Given
        AuthProvider provider = new NoAuth();

        // Then
        assertThat(provider).isNotNull();
        assertThat(provider.getName()).isEqualTo("no_auth");
        assertThat(provider.passwordEncoder()).isNotNull();
    }

    @Test
    void shouldReturnBCryptPasswordEncoder() {
        // Given
        NoAuth provider = new NoAuth();

        // When
        var encoder = provider.passwordEncoder();

        // Then
        assertThat(encoder).isNotNull();
        assertThat(encoder.getClass().getSimpleName()).isEqualTo("BCryptPasswordEncoder");
        
        // Verify it can encode and verify passwords
        String rawPassword = "testPassword";
        String encodedPassword = encoder.encode(rawPassword);
        assertThat(encodedPassword).isNotEqualTo(rawPassword);
        assertThat(encoder.matches(rawPassword, encodedPassword)).isTrue();
    }
}
