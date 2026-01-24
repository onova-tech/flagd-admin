package tech.onova.flagd_admin_server.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
public class UserDetailsConfiguration {

    @Bean
    public UserDetailsService jwtAuthUserDetailsService(
            @Value("${application.auth.provider.jwt.admin.name}") String userName,
            @Value("${application.auth.provider.jwt.admin.encoded_password}") String userEncodedPassword) {
        User.UserBuilder userBuilder = User.builder()
                .username(userName)
                .password(userEncodedPassword)
                .roles("ADMIN");
        return new InMemoryUserDetailsManager(userBuilder.build());
    }
}