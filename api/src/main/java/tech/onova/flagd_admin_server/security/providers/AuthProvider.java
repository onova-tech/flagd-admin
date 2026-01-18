package tech.onova.flagd_admin_server.security.providers;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

public interface AuthProvider {
    String getName();

    SecurityFilterChain securityFilterChain(HttpSecurity http, String redirectUri) throws Exception;
    UserDetailsService userDetailsService(PasswordEncoder passwordEncoder);
    PasswordEncoder passwordEncoder();
}
