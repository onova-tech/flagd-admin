package tech.onova.flagd_admin_server.security.providers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Component
public class BasicAuthProvider implements AuthProvider {
    @Value("${application.auth.provider.basic.user.name}")
    private String userName;

    @Value("${application.auth.provider.basic.user.encoded_password}")
    private String userEncodedPassword;

    @Override
    public String getName() {
        return "basic";
    }

    @Override
    public SecurityFilterChain securityFilterChain(HttpSecurity http, String redirectUri) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // Allow public access to the home page and sign-up endpoint
                        .requestMatchers("/login").permitAll()
                        // Require authentication for all other requests
                        .anyRequest().authenticated()
                )
                // Configure form-based login
                .formLogin(form -> form.defaultSuccessUrl(redirectUri, true))
                // Configure logout
                .logout(LogoutConfigurer::permitAll)
                .cors(httpSecurityCorsConfigurer ->
                        httpSecurityCorsConfigurer.configurationSource(corsConfigurationSource()));

        // This is a minimal example; additional settings for CSRF, CORS, etc.,
        // can also be configured using the http object.
        return http.build();
    }

    @Override
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        // The passwordEncoder bean automatically handles the hashing and prefixing if used correctly
        UserDetails user = User.builder()
                .username(userName)
                .password(userEncodedPassword) // Spring hashes the raw password
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Override
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
