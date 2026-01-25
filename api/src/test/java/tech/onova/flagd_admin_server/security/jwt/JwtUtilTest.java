package tech.onova.flagd_admin_server.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private UserDetails userDetails;
    
    private static final String TEST_SECRET = "test-secret-key-that-is-long-enough-for-hmac-sha-256-algorithm";
    private static final long ACCESS_TOKEN_EXPIRATION = 900000L; // 15 minutes
    private static final long REFRESH_TOKEN_EXPIRATION = 604800000L; // 7 days
    private static final String TEST_USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", ACCESS_TOKEN_EXPIRATION);
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpiration", REFRESH_TOKEN_EXPIRATION);
        
        userDetails = User.builder()
            .username(TEST_USERNAME)
            .password("password")
            .roles("USER")
            .build();
    }

    @Test
    void generateAccessToken_shouldCreateValidToken() {
        // When
        String token = jwtUtil.generateAccessToken(userDetails);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        
        String extractedUsername = jwtUtil.extractUsername(token);
        assertThat(extractedUsername).isEqualTo(TEST_USERNAME);
        
        Date expiration = jwtUtil.extractExpiration(token);
        assertThat(expiration).isAfter(new Date());
        
        boolean isValid = jwtUtil.validateToken(token, userDetails);
        assertThat(isValid).isTrue();
    }

    @Test
    void generateRefreshToken_shouldCreateValidToken() {
        // When
        String token = jwtUtil.generateRefreshToken(userDetails);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        
        String extractedUsername = jwtUtil.extractUsername(token);
        assertThat(extractedUsername).isEqualTo(TEST_USERNAME);
        
        Date expiration = jwtUtil.extractExpiration(token);
        assertThat(expiration).isAfter(new Date());
        
        boolean isValid = jwtUtil.validateToken(token, userDetails);
        assertThat(isValid).isTrue();
        
        boolean isRefreshToken = jwtUtil.isRefreshToken(token);
        assertThat(isRefreshToken).isTrue();
    }

    @Test
    void extractUsername_shouldReturnCorrectUsername() {
        // Given
        String token = jwtUtil.generateAccessToken(userDetails);

        // When
        String username = jwtUtil.extractUsername(token);

        // Then
        assertThat(username).isEqualTo(TEST_USERNAME);
    }

    @Test
    void extractExpiration_shouldReturnCorrectExpiration() {
        // Given
        Date beforeGeneration = new Date();
        String token = jwtUtil.generateAccessToken(userDetails);
        Date afterGeneration = new Date();

        // When
        Date expiration = jwtUtil.extractExpiration(token);

        // Then
        assertThat(expiration).isAfter(afterGeneration);
        long expectedMinExpiration = beforeGeneration.getTime() + ACCESS_TOKEN_EXPIRATION;
        long expectedMaxExpiration = afterGeneration.getTime() + ACCESS_TOKEN_EXPIRATION;
        assertThat(expiration.getTime()).isBetween(expectedMinExpiration - 5000, expectedMaxExpiration + 5000); // 5 second tolerance
    }

    @Test
    void extractClaim_shouldExtractCustomClaims() {
        // Given
        String token = jwtUtil.generateAccessToken(userDetails);

        // When
        Claims claims = jwtUtil.extractClaim(token, c -> c);

        // Then
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo(TEST_USERNAME);
        assertThat(claims.get("type")).isEqualTo("access");
    }

    @Test
    void validateToken_shouldReturnTrueForValidToken() {
        // Given
        String token = jwtUtil.generateAccessToken(userDetails);

        // When
        boolean isValid = jwtUtil.validateToken(token, userDetails);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void validateToken_shouldReturnFalseForExpiredToken() {
        // Given
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", -1000L); // Expired token
        String token = jwtUtil.generateAccessToken(userDetails);

        // When
        boolean isValid = false;
        try {
            isValid = jwtUtil.validateToken(token, userDetails);
        } catch (Exception e) {
            // Expected for expired token
            isValid = false;
        }

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void validateToken_shouldReturnFalseForWrongUser() {
        // Given
        String token = jwtUtil.generateAccessToken(userDetails);
        UserDetails otherUser = User.builder()
            .username("otheruser")
            .password("password")
            .roles("USER")
            .build();

        // When
        boolean isValid = jwtUtil.validateToken(token, otherUser);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void isTokenExpired_shouldReturnTrueForExpiredToken() {
        // Given
        // Create a manually crafted expired token
        Date pastDate = new Date(System.currentTimeMillis() - 1000); // 1 second ago
        String token = Jwts.builder()
            .setSubject(TEST_USERNAME)
            .setIssuedAt(new Date())
            .setExpiration(pastDate)
            .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)))
            .compact();

        // When & Then
        // The validateToken method should throw ExpiredJwtException for expired tokens
        // This is expected behavior - expired tokens should not be valid
        assertThatThrownBy(() -> jwtUtil.validateToken(token, userDetails))
            .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    void isRefreshToken_shouldReturnTrueForRefreshToken() {
        // Given
        String token = jwtUtil.generateRefreshToken(userDetails);

        // When
        boolean isRefreshToken = jwtUtil.isRefreshToken(token);

        // Then
        assertThat(isRefreshToken).isTrue();
    }

    @Test
    void isRefreshToken_shouldReturnFalseForAccessToken() {
        // Given
        String token = jwtUtil.generateAccessToken(userDetails);

        // When
        boolean isRefreshToken = jwtUtil.isRefreshToken(token);

        // Then
        assertThat(isRefreshToken).isFalse();
    }

    @Test
    void getSigningKey_shouldCreateValidKey() {
        // Given
        SecretKey expectedKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));

        // When
        SecretKey signingKey = (SecretKey) ReflectionTestUtils.invokeMethod(jwtUtil, "getSigningKey");

        // Then
        assertThat(signingKey).isNotNull();
        assertThat(signingKey.getAlgorithm()).isEqualTo(expectedKey.getAlgorithm());
        assertThat(signingKey.getEncoded()).isEqualTo(expectedKey.getEncoded());
    }

    @Test
    void extractAllClaims_shouldParseMalformedToken_shouldThrowException() {
        // Given
        String malformedToken = "this.is.not.a.valid.jwt.token";

        // When & Then
        assertThatThrownBy(() -> jwtUtil.extractClaim(malformedToken, c -> c))
            .isInstanceOf(Exception.class);
    }

    @Test
    void extractAllClaims_shouldParseTokenWithWrongSignature_shouldThrowException() {
        // Given
        String validToken = jwtUtil.generateAccessToken(userDetails);
        String tamperedToken = validToken.substring(0, validToken.length() - 5) + "wrong";

        // When & Then
        assertThatThrownBy(() -> jwtUtil.extractClaim(tamperedToken, c -> c))
            .isInstanceOf(Exception.class);
    }

    @Test
    void generateToken_shouldIncludeExtraClaims() {
        // Given
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("customClaim", "customValue");
        extraClaims.put("userId", "12345");

        // When
        String token = (String) ReflectionTestUtils.invokeMethod(
            jwtUtil, "generateToken", extraClaims, userDetails, ACCESS_TOKEN_EXPIRATION);

        // Then
        Claims claims = jwtUtil.extractClaim(token, c -> c);
        assertThat(claims.get("customClaim")).isEqualTo("customValue");
        assertThat(claims.get("userId")).isEqualTo("12345");
        assertThat(claims.getSubject()).isEqualTo(TEST_USERNAME);
    }

    @Test
    void generateToken_shouldSetCorrectTimestamps() {
        // Given
        Date beforeGeneration = new Date();
        String token = jwtUtil.generateAccessToken(userDetails);
        Date afterGeneration = new Date();

        // When
        Claims claims = jwtUtil.extractClaim(token, c -> c);

        // Then
        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();
        long duration = expiration.getTime() - issuedAt.getTime();
        assertThat(duration).isCloseTo(ACCESS_TOKEN_EXPIRATION, within(2000L)); // 2 second tolerance
    }

    @Test
    void validateToken_shouldHandleNullToken() {
        // When & Then
        assertThatThrownBy(() -> jwtUtil.validateToken(null, userDetails))
            .isInstanceOf(Exception.class);
    }

    @Test
    void validateToken_shouldHandleEmptyToken() {
        // When & Then
        assertThatThrownBy(() -> jwtUtil.validateToken("", userDetails))
            .isInstanceOf(Exception.class);
    }

    @Test
    void generateTokens_shouldCreateDifferentTokenTypes() {
        // When
        String accessToken = jwtUtil.generateAccessToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        // Then
        assertThat(accessToken).isNotEqualTo(refreshToken);
        
        boolean isAccessRefresh = jwtUtil.isRefreshToken(accessToken);
        boolean isRefreshRefresh = jwtUtil.isRefreshToken(refreshToken);
        
        assertThat(isAccessRefresh).isFalse();
        assertThat(isRefreshRefresh).isTrue();
        
        // Both should be valid for the same user
        assertThat(jwtUtil.validateToken(accessToken, userDetails)).isTrue();
        assertThat(jwtUtil.validateToken(refreshToken, userDetails)).isTrue();
    }

    @Test
    void extractAllClaims_shouldWorkWithDifferentSecretKeys() {
        // Given
        String token = jwtUtil.generateAccessToken(userDetails);
        String differentSecret = "different-secret-key-that-is-long-enough";
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", differentSecret);

        // When & Then
        assertThatThrownBy(() -> jwtUtil.extractClaim(token, claims -> claims))
            .isInstanceOf(Exception.class);
    }
}