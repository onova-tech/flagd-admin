package tech.onova.flagd_admin_server.security.providers;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class AuthProviderInterfaceTest {

    @Test
    void noAuthProvider_ShouldImplementAuthProvider() {
        // When
        AuthProvider provider = new NoAuth();

        // Then
        assertThat(provider).isNotNull();
        assertThat(provider.getName()).isEqualTo("no_auth");
        assertThat(provider.passwordEncoder()).isNotNull();
    }

    @Test
    void basicAuthProvider_ShouldImplementAuthProvider() {
        // When
        AuthProvider provider = new BasicAuthProvider();

        // Then
        assertThat(provider).isNotNull();
        assertThat(provider.getName()).isEqualTo("basic");
        assertThat(provider.passwordEncoder()).isNotNull();
    }
}
