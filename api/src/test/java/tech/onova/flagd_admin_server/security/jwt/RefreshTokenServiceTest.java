package tech.onova.flagd_admin_server.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;
import tech.onova.flagd_admin_server.domain.entity.RefreshToken;
import tech.onova.flagd_admin_server.domain.repository.RefreshTokenRepository;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private static final long REFRESH_TOKEN_EXPIRATION = 604800000L; // 7 days in milliseconds
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_TOKEN_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpiration", REFRESH_TOKEN_EXPIRATION);
        when(userDetails.getUsername()).thenReturn(TEST_USERNAME);
    }

    @Test
    void createRefreshToken_shouldCreateTokenWithValidExpiration() {
        // Given
        RefreshToken expectedToken = new RefreshToken(TEST_TOKEN_ID, TEST_USERNAME, 
            ZonedDateTime.now().plusDays(7));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(expectedToken);

        // When
        RefreshToken result = refreshTokenService.createRefreshToken(userDetails);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(TEST_USERNAME);
        assertThat(result.getExpiresAt()).isAfter(ZonedDateTime.now());
        assertThat(result.getExpiresAt()).isBefore(ZonedDateTime.now().plusDays(8));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void findByToken_shouldReturnTokenWhenExists() {
        // Given
        RefreshToken expectedToken = new RefreshToken(TEST_TOKEN_ID, TEST_USERNAME, 
            ZonedDateTime.now().plusDays(7));
        when(refreshTokenRepository.findById(TEST_TOKEN_ID)).thenReturn(Optional.of(expectedToken));

        // When
        RefreshToken result = refreshTokenService.findByToken(TEST_TOKEN_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(TEST_TOKEN_ID);
        assertThat(result.getUserId()).isEqualTo(TEST_USERNAME);
        verify(refreshTokenRepository).findById(TEST_TOKEN_ID);
    }

    @Test
    void findByToken_shouldReturnNullWhenNotExists() {
        // Given
        when(refreshTokenRepository.findById(TEST_TOKEN_ID)).thenReturn(Optional.empty());

        // When
        RefreshToken result = refreshTokenService.findByToken(TEST_TOKEN_ID);

        // Then
        assertThat(result).isNull();
        verify(refreshTokenRepository).findById(TEST_TOKEN_ID);
    }

    @Test
    void verifyExpiration_shouldReturnTokenWhenValid() {
        // Given
        RefreshToken validToken = new RefreshToken(TEST_TOKEN_ID, TEST_USERNAME, 
            ZonedDateTime.now().plusDays(1));

        // When
        RefreshToken result = refreshTokenService.verifyExpiration(validToken);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(TEST_TOKEN_ID);
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void verifyExpiration_shouldThrowExceptionAndDeleteTokenWhenExpired() {
        // Given
        RefreshToken expiredToken = new RefreshToken(TEST_TOKEN_ID, TEST_USERNAME, 
            ZonedDateTime.now().minusDays(1));

        // When & Then
        assertThatThrownBy(() -> refreshTokenService.verifyExpiration(expiredToken))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Refresh token was expired. Please make a new login request");
        
        verify(refreshTokenRepository).delete(expiredToken);
    }

    @Test
    void deleteByUserId_shouldDeleteOnlyUserTokens() {
        // Given
        RefreshToken userToken1 = new RefreshToken("token1", TEST_USERNAME, ZonedDateTime.now().plusDays(1));
        RefreshToken userToken2 = new RefreshToken("token2", TEST_USERNAME, ZonedDateTime.now().plusDays(1));
        RefreshToken otherUserToken = new RefreshToken("token3", "otheruser", ZonedDateTime.now().plusDays(1));
        
        List<RefreshToken> allTokens = Arrays.asList(userToken1, userToken2, otherUserToken);
        when(refreshTokenRepository.findAll()).thenReturn(allTokens);

        // When
        refreshTokenService.deleteByUserId(TEST_USERNAME);

        // Then
        verify(refreshTokenRepository).delete(userToken1);
        verify(refreshTokenRepository).delete(userToken2);
        verify(refreshTokenRepository, never()).delete(otherUserToken);
    }

    @Test
    void deleteByUserId_shouldDoNothingWhenUserHasNoTokens() {
        // Given
        RefreshToken otherUserToken = new RefreshToken("token3", "otheruser", ZonedDateTime.now().plusDays(1));
        List<RefreshToken> allTokens = Arrays.asList(otherUserToken);
        when(refreshTokenRepository.findAll()).thenReturn(allTokens);

        // When
        refreshTokenService.deleteByUserId(TEST_USERNAME);

        // Then
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void deleteByUserId_shouldHandleEmptyTokenList() {
        // Given
        when(refreshTokenRepository.findAll()).thenReturn(Arrays.asList());

        // When
        refreshTokenService.deleteByUserId(TEST_USERNAME);

        // Then
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void deleteRefreshToken_shouldDeleteToken() {
        // Given
        RefreshToken tokenToDelete = new RefreshToken(TEST_TOKEN_ID, TEST_USERNAME, 
            ZonedDateTime.now().plusDays(1));

        // When
        refreshTokenService.deleteRefreshToken(tokenToDelete);

        // Then
        verify(refreshTokenRepository).delete(tokenToDelete);
    }

    @Test
    void createRefreshToken_shouldUseConfiguredExpirationTime() {
        // Given
        long customExpiration = 3600000L; // 1 hour
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpiration", customExpiration);
        
        RefreshToken expectedToken = new RefreshToken(TEST_TOKEN_ID, TEST_USERNAME, 
            ZonedDateTime.now().plusSeconds(customExpiration / 1000));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(expectedToken);

        // When
        ZonedDateTime beforeCreation = ZonedDateTime.now();
        RefreshToken result = refreshTokenService.createRefreshToken(userDetails);
        ZonedDateTime afterCreation = ZonedDateTime.now();

        // Then
        assertThat(result.getExpiresAt()).isAfter(beforeCreation.plusSeconds(customExpiration / 1000 - 1)); // Allow 1 second tolerance
        assertThat(result.getExpiresAt()).isBefore(afterCreation.plusSeconds(customExpiration / 1000 + 1));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void createRefreshToken_shouldGenerateUniqueTokenIds() {
        // Given
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        RefreshToken token1 = refreshTokenService.createRefreshToken(userDetails);
        RefreshToken token2 = refreshTokenService.createRefreshToken(userDetails);

        // Then
        assertThat(token1.getId()).isNotEqualTo(token2.getId());
        assertThat(token1.getUserId()).isEqualTo(token2.getUserId()).isEqualTo(TEST_USERNAME);
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }
}