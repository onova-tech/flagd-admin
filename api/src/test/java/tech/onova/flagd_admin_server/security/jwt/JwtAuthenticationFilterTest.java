package tech.onova.flagd_admin_server.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final String TEST_USERNAME = "testuser";
    private final String VALID_TOKEN = "valid.jwt.token";
    private final String INVALID_TOKEN = "invalid.jwt.token";
    private final String BEARER_TOKEN = "Bearer " + VALID_TOKEN;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        userDetails = User.builder()
            .username(TEST_USERNAME)
            .password("password")
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
            .build();
        
        // Add common stubs
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    @Test
    void doFilterInternal_shouldSkipFilterWhenNoAuthHeader() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtUtil, never()).extractUsername(anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_shouldSkipFilterWhenAuthHeaderDoesNotStartWithBearer() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Basic dGVzdDp0ZXN0");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtUtil, never()).extractUsername(anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_shouldSkipFilterWhenUsernameIsNull() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(jwtUtil.extractUsername(VALID_TOKEN)).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtUtil).extractUsername(VALID_TOKEN);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_shouldSkipFilterWhenAuthenticationExists() throws ServletException, IOException {
        // Given
        UsernamePasswordAuthenticationToken existingAuth = 
            new UsernamePasswordAuthenticationToken("existinguser", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(jwtUtil.extractUsername(VALID_TOKEN)).thenReturn(TEST_USERNAME);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtUtil).extractUsername(VALID_TOKEN);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        
        // Existing authentication should remain
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(existingAuth);
    }

    @Test
    void doFilterInternal_shouldAuthenticateWhenValidToken() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(jwtUtil.extractUsername(VALID_TOKEN)).thenReturn(TEST_USERNAME);
        when(userDetailsService.loadUserByUsername(TEST_USERNAME)).thenReturn(userDetails);
        when(jwtUtil.validateToken(VALID_TOKEN, userDetails)).thenReturn(true);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtUtil).extractUsername(VALID_TOKEN);
        verify(userDetailsService).loadUserByUsername(TEST_USERNAME);
        verify(jwtUtil).validateToken(VALID_TOKEN, userDetails);

        UsernamePasswordAuthenticationToken authentication = 
            (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(userDetails);
        assertThat(authentication.getCredentials()).isNull();
        assertThat(authentication.getAuthorities()).containsExactlyElementsOf(userDetails.getAuthorities());
        assertThat(authentication.getDetails()).isNotNull();
    }

    @Test
    void doFilterInternal_shouldNotAuthenticateWhenInvalidToken() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer " + INVALID_TOKEN);
        when(jwtUtil.extractUsername(INVALID_TOKEN)).thenReturn(TEST_USERNAME);
        when(userDetailsService.loadUserByUsername(TEST_USERNAME)).thenReturn(userDetails);
        when(jwtUtil.validateToken(INVALID_TOKEN, userDetails)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtUtil).extractUsername(INVALID_TOKEN);
        verify(userDetailsService).loadUserByUsername(TEST_USERNAME);
        verify(jwtUtil).validateToken(INVALID_TOKEN, userDetails);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_shouldHandleUserDetailsServiceException() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(jwtUtil.extractUsername(VALID_TOKEN)).thenReturn(TEST_USERNAME);
        when(userDetailsService.loadUserByUsername(TEST_USERNAME))
            .thenThrow(new RuntimeException("User not found"));

        // When & Then
        // The current implementation doesn't handle exceptions from userDetailsService
        // so, exception should propagate. This test documents the current behavior.
        assertThatThrownBy(() -> jwtAuthenticationFilter.doFilterInternal(request, response, filterChain))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("User not found");
        
        verify(jwtUtil).extractUsername(VALID_TOKEN);
        verify(userDetailsService).loadUserByUsername(TEST_USERNAME);
        verify(jwtUtil, never()).validateToken(anyString(), any());
        verify(filterChain, never()).doFilter(request, response);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_shouldHandleJwtUtilException() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(jwtUtil.extractUsername(VALID_TOKEN))
            .thenThrow(new RuntimeException("Invalid JWT token"));

        // When & Then
        // The current implementation doesn't handle exceptions from jwtUtil.extractUsername
        // so the exception should propagate. This test documents the current behavior.
        assertThatThrownBy(() -> jwtAuthenticationFilter.doFilterInternal(request, response, filterChain))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Invalid JWT token");
        
        verify(jwtUtil).extractUsername(VALID_TOKEN);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(jwtUtil, never()).validateToken(anyString(), any());
        verify(filterChain, never()).doFilter(request, response);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_shouldHandleMalformedBearerToken() throws ServletException, IOException {
        // Given
        String malformedToken = "Bearer"; // No space after Bearer
        when(request.getHeader("Authorization")).thenReturn(malformedToken);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtUtil, never()).extractUsername(anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_shouldHandleEmptyBearerToken() throws ServletException, IOException {
        // Given
        String emptyToken = "Bearer ";
        when(request.getHeader("Authorization")).thenReturn(emptyToken);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        // The filter will call extractUsername with empty string
        verify(jwtUtil).extractUsername("");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_shouldSetAuthenticationDetails() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(jwtUtil.extractUsername(VALID_TOKEN)).thenReturn(TEST_USERNAME);
        when(userDetailsService.loadUserByUsername(TEST_USERNAME)).thenReturn(userDetails);
        when(jwtUtil.validateToken(VALID_TOKEN, userDetails)).thenReturn(true);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        UsernamePasswordAuthenticationToken authentication = 
            (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        
        assertThat(authentication).isNotNull();
        assertThat(authentication.getDetails()).isNotNull();
    }

    @Test
    void doFilterInternal_shouldNotOverrideExistingAuthenticationWhenUsernameMatches() throws ServletException, IOException {
        // Given
        UsernamePasswordAuthenticationToken existingAuth = 
            new UsernamePasswordAuthenticationToken(TEST_USERNAME, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(jwtUtil.extractUsername(VALID_TOKEN)).thenReturn(TEST_USERNAME);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtUtil).extractUsername(VALID_TOKEN);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        
        // Existing authentication should remain unchanged
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(existingAuth);
    }

    @Test
    void doFilterInternal_shouldExtractTokenCorrectly() throws ServletException, IOException {
        // Given
        String fullHeader = "Bearer " + VALID_TOKEN;
        when(request.getHeader("Authorization")).thenReturn(fullHeader);
        when(jwtUtil.extractUsername(VALID_TOKEN)).thenReturn(TEST_USERNAME);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtUtil).extractUsername(VALID_TOKEN); // Should be called with token without "Bearer "
        verify(jwtUtil, never()).extractUsername(fullHeader); // Should not be called with full header
    }
}