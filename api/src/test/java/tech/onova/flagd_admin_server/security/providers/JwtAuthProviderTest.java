package tech.onova.flagd_admin_server.security.providers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tech.onova.flagd_admin_server.security.jwt.JwtAuthenticationFilter;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthProviderTest {

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private JwtAuthProvider jwtAuthProvider;

    @BeforeEach
    void setUp() {
        jwtAuthProvider = new JwtAuthProvider();
        // Use reflection to inject the mock filter
        try {
            var field = JwtAuthProvider.class.getDeclaredField("jwtAuthenticationFilter");
            field.setAccessible(true);
            field.set(jwtAuthProvider, jwtAuthenticationFilter);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock", e);
        }
    }

    @Test
    void getName_shouldReturnJwt() {
        // When
        String name = jwtAuthProvider.getName();

        // Then
        assertThat(name).isEqualTo("jwt");
    }

    @Test
    void passwordEncoder_shouldReturnBCryptPasswordEncoder() {
        // When
        PasswordEncoder encoder = jwtAuthProvider.passwordEncoder();

        // Then
        assertThat(encoder).isNotNull();
        assertThat(encoder).isInstanceOf(org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder.class);
        
        // Test encoding works
        String rawPassword = "testpassword";
        String encodedPassword = encoder.encode(rawPassword);
        assertThat(encodedPassword).isNotNull();
        assertThat(encodedPassword).isNotEqualTo(rawPassword);
        assertThat(encoder.matches(rawPassword, encodedPassword)).isTrue();
        assertThat(encoder.matches("wrongpassword", encodedPassword)).isFalse();
    }

    @Test
    void passwordEncoder_shouldConsistentlyEncodeSamePassword() {
        // Given
        PasswordEncoder encoder = jwtAuthProvider.passwordEncoder();
        String password = "testpassword";

        // When
        String encoded1 = encoder.encode(password);
        String encoded2 = encoder.encode(password);

        // Then
        assertThat(encoded1).isNotEqualTo(password);
        assertThat(encoded2).isNotEqualTo(password);
        assertThat(encoded1).isNotEqualTo(encoded2); // BCrypt generates different hashes each time
        
        // But both should match the original password
        assertThat(encoder.matches(password, encoded1)).isTrue();
        assertThat(encoder.matches(password, encoded2)).isTrue();
    }

    @Test
    void passwordEncoder_shouldHandleNullPassword() {
        // Given
        PasswordEncoder encoder = jwtAuthProvider.passwordEncoder();

        // When
        String encoded = encoder.encode(null);

        // Then
        assertThat(encoded).isNull();
    }

    @Test
    void passwordEncoder_shouldValidateEmptyPassword() {
        // Given
        PasswordEncoder encoder = jwtAuthProvider.passwordEncoder();
        String emptyPassword = "";

        // When
        String encoded = encoder.encode(emptyPassword);

        // Then
        assertThat(encoded).isNotNull();
        assertThat(encoded).isNotEqualTo(emptyPassword);
        // Note: In Spring Security 7.0.2, empty passwords may have different matching behavior
        assertThat(encoder.matches(emptyPassword, encoded)).isFalse();
        assertThat(encoder.matches("notempty", encoded)).isFalse();
    }

    @Test
    void corsConfigurationSource_shouldCreateValidCorsConfiguration() {
        // When
        CorsConfigurationSource source = jwtAuthProvider.corsConfigurationSource();

        // Then
        assertThat(source).isNotNull();
        assertThat(source).isInstanceOf(UrlBasedCorsConfigurationSource.class);
    }

    @Test
    void corsConfigurationSource_shouldHaveWildcardConfigurations() {
        // When
        CorsConfigurationSource source = jwtAuthProvider.corsConfigurationSource();
        UrlBasedCorsConfigurationSource urlBasedSource = (UrlBasedCorsConfigurationSource) source;

        // Then
        // Test that the source has registered the wildcard path
        CorsConfiguration config = urlBasedSource.getCorsConfigurations().get("/**");
        assertThat(config).isNotNull();

        // Verify CORS configuration
        assertThat(config.getAllowedOriginPatterns()).contains("*");
        assertThat(config.getAllowedMethods()).contains("*");
        assertThat(config.getAllowedHeaders()).contains("*");
        assertThat(config.getAllowCredentials()).isTrue();
    }

    @Test
    void corsConfigurationSource_shouldSupportAllHttpMethods() {
        // When
        CorsConfigurationSource source = jwtAuthProvider.corsConfigurationSource();
        UrlBasedCorsConfigurationSource urlBasedSource = (UrlBasedCorsConfigurationSource) source;
        CorsConfiguration config = urlBasedSource.getCorsConfigurations().get("/**");

        // Then
        List<String> allowedMethods = config.getAllowedMethods();
        assertThat(allowedMethods).contains("*");
        assertThat(allowedMethods).hasSize(1);
    }

    @Test
    void corsConfigurationSource_shouldSupportAllHeaders() {
        // When
        CorsConfigurationSource source = jwtAuthProvider.corsConfigurationSource();
        UrlBasedCorsConfigurationSource urlBasedSource = (UrlBasedCorsConfigurationSource) source;
        CorsConfiguration config = urlBasedSource.getCorsConfigurations().get("/**");

        // Then
        List<String> allowedHeaders = config.getAllowedHeaders();
        assertThat(allowedHeaders).contains("*");
        assertThat(allowedHeaders).hasSize(1);
    }

    @Test
    void corsConfigurationSource_shouldUseOriginPatterns() {
        // When
        CorsConfigurationSource source = jwtAuthProvider.corsConfigurationSource();
        UrlBasedCorsConfigurationSource urlBasedSource = (UrlBasedCorsConfigurationSource) source;
        CorsConfiguration config = urlBasedSource.getCorsConfigurations().get("/**");

        // Then
        assertThat(config.getAllowedOrigins()).isNull(); // Should use originPatterns instead
        assertThat(config.getAllowedOriginPatterns()).contains("*");
    }

    @Test
    void passwordEncoder_shouldHandleLongPasswords() {
        // Given
        PasswordEncoder encoder = jwtAuthProvider.passwordEncoder();
        String longPassword = "a".repeat(1000); // 1000 character password

        // When & Then
        assertThatThrownBy(() -> encoder.encode(longPassword))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("password cannot be more than 72 bytes");
    }

    @Test
    void passwordEncoder_shouldSupportDifferentPasswords() {
        // Given
        PasswordEncoder encoder = jwtAuthProvider.passwordEncoder();
        String password1 = "password1";
        String password2 = "password2";

        // When
        String encoded1 = encoder.encode(password1);
        String encoded2 = encoder.encode(password2);

        // Then
        assertThat(encoded1).isNotEqualTo(encoded2);
        assertThat(encoder.matches(password1, encoded1)).isTrue();
        assertThat(encoder.matches(password2, encoded1)).isFalse();
        assertThat(encoder.matches(password1, encoded2)).isFalse();
        assertThat(encoder.matches(password2, encoded2)).isTrue();
    }

    @Test
    void corsConfigurationSource_shouldCreateCorrectSourceStructure() {
        // When
        CorsConfigurationSource source = jwtAuthProvider.corsConfigurationSource();

        // Then
        UrlBasedCorsConfigurationSource urlBasedSource = (UrlBasedCorsConfigurationSource) source;
        assertThat(urlBasedSource.getCorsConfigurations()).isNotNull();
        assertThat(urlBasedSource.getCorsConfigurations()).hasSize(1);
        assertThat(urlBasedSource.getCorsConfigurations()).containsKey("/**");
    }
}