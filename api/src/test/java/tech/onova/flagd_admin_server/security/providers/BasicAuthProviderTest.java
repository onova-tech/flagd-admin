package tech.onova.flagd_admin_server.security.providers;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class BasicAuthProviderTest {

    @Test
    void shouldImplementAuthProvider() {
        // Given
        AuthProvider provider = new BasicAuthProvider();

        // Then
        assertThat(provider).isNotNull();
        assertThat(provider.getName()).isEqualTo("basic");
        assertThat(provider.passwordEncoder()).isNotNull();
    }

    @Test
    void shouldReturnBCryptPasswordEncoder() {
        // Given
        BasicAuthProvider provider = new BasicAuthProvider();

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
