package tech.onova.flagd_admin_server.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import tech.onova.flagd_admin_server.security.providers.AuthProvider;
import tech.onova.flagd_admin_server.security.providers.BasicAuthProvider;

import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private static final String DEFAULT_AUTH_PROVIDER = "basic";
    private final AuthProvider authProvider;

    @Value("${application.auth.login.default_redirect_uri}")
    private String redirectUri;

    @Autowired
    public SecurityConfig(List<AuthProvider> authProviders,
                          @Value("${application.auth.provider}") String authProviderName) {
        var basicAuthProvider = authProviders.stream()
                .filter(provider -> provider.getName().equalsIgnoreCase(DEFAULT_AUTH_PROVIDER))
                .findFirst()
                .orElseThrow();

        this.authProvider = authProviders.stream()
                .filter(provider -> provider.getName().equalsIgnoreCase(authProviderName))
                .findFirst()
                .orElse(basicAuthProvider);
    }

    // 1. Configures the security filter chain for HTTP requests
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return authProvider.securityFilterChain(http, redirectUri);
    }

    // 2. Configures an in-memory user store for demonstration purposes
    // In a real application, you would use a database-backed UserDetailsService
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        return authProvider.userDetailsService(passwordEncoder);
    }

    // 3. Configures a password encoder bean
    @Bean
    public PasswordEncoder passwordEncoder() {
        return authProvider.passwordEncoder();
    }
}