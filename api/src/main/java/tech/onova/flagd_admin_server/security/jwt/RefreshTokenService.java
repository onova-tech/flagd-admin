package tech.onova.flagd_admin_server.security.jwt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import tech.onova.flagd_admin_server.domain.entity.RefreshToken;
import tech.onova.flagd_admin_server.domain.repository.RefreshTokenRepository;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Service
public class RefreshTokenService {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Value("${application.auth.provider.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    public RefreshToken createRefreshToken(UserDetails userDetails) {
        String tokenId = UUID.randomUUID().toString();
        ZonedDateTime expiresAt = ZonedDateTime.now().plus(Duration.ofMillis(refreshTokenExpiration));
        RefreshToken refreshToken = new RefreshToken(tokenId, userDetails.getUsername(), expiresAt);
        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findById(token).orElse(null);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiresAt().isBefore(ZonedDateTime.now())) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token was expired. Please make a new login request");
        }
        return token;
    }

    public void deleteByUserId(String userId) {
        Iterable<RefreshToken> tokens = refreshTokenRepository.findAll();
        StreamSupport.stream(tokens.spliterator(), false)
                .filter(token -> token.getUserId().equals(userId))
                .forEach(refreshTokenRepository::delete);
    }

    public void deleteRefreshToken(RefreshToken token) {
        refreshTokenRepository.delete(token);
    }
}