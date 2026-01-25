package tech.onova.flagd_admin_server.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.password.PasswordEncoder;
import tech.onova.flagd_admin_server.security.providers.AuthProvider;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    private static final String DEFAULT_AUTH_PROVIDER = "jwt";
    private final AuthProvider authProvider;

    @Value("${application.auth.login.default_redirect_uri}")
    private String redirectUri;

    @Autowired
    public SecurityConfiguration(List<AuthProvider> authProviders,
                                 @Value("${application.auth.provider}") String authProviderName) {
        this.authProvider = authProviders.stream()
                .filter(provider -> provider.getName().equalsIgnoreCase(authProviderName))
                .filter(provider -> provider.getName().equalsIgnoreCase(DEFAULT_AUTH_PROVIDER))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Authentication provider '" + authProviderName + "' not found"));
    }

    // Configures the security filter chain for HTTP requests
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return authProvider.securityFilterChain(http, redirectUri);
    }

    // Configures a password encoder bean
    @Bean
    public PasswordEncoder passwordEncoder() {
        return authProvider.passwordEncoder();
    }

    // Configures the AuthenticationManager bean
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http, 
                                                      UserDetailsService userDetailsService, 
                                                      PasswordEncoder passwordEncoder) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = 
            http.getSharedObject(AuthenticationManagerBuilder.class);
        
        authenticationManagerBuilder
            .userDetailsService(userDetailsService)
            .passwordEncoder(passwordEncoder);
        
        return authenticationManagerBuilder.build();
    }

}