package tech.onova.flagd_admin_server.security.providers;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class AuthProviderInterfaceTest {

    @Test
    void jwtAuthProvider_ShouldImplementAuthProvider() {
        // When
        AuthProvider provider = new JwtAuthProvider();

        // Then
        assertThat(provider).isNotNull();
        assertThat(provider.getName()).isEqualTo("jwt");
        assertThat(provider.passwordEncoder()).isNotNull();
    }
}
