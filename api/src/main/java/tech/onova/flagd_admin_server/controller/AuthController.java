package tech.onova.flagd_admin_server.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import tech.onova.flagd_admin_server.controller.DTOs.LoginRequestDTO;
import tech.onova.flagd_admin_server.controller.DTOs.LoginResponseDTO;
import tech.onova.flagd_admin_server.controller.DTOs.RefreshTokenRequestDTO;
import tech.onova.flagd_admin_server.security.jwt.JwtUtil;
import tech.onova.flagd_admin_server.security.jwt.RefreshTokenService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(request.username())
                .password(request.password())
                .build();

        String accessToken = jwtUtil.generateAccessToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        tech.onova.flagd_admin_server.domain.entity.RefreshToken refreshTokenEntity =
                refreshTokenService.createRefreshToken(userDetails);

        LoginResponseDTO response = new LoginResponseDTO(
                accessToken,
                refreshTokenEntity.getId(),
                "Bearer"
        );

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDTO> refreshToken(@Valid @RequestBody RefreshTokenRequestDTO request) {
        String refreshTokenId = request.refreshToken();

        tech.onova.flagd_admin_server.domain.entity.RefreshToken refreshTokenEntity =
                refreshTokenService.findByToken(refreshTokenId);

        if (refreshTokenEntity == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        tech.onova.flagd_admin_server.domain.entity.RefreshToken verifiedRefreshToken =
                refreshTokenService.verifyExpiration(refreshTokenEntity);

        String username = verifiedRefreshToken.getUserId();

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(username)
                .password("")
                .build();

        String newAccessToken = jwtUtil.generateAccessToken(userDetails);
        String newRefreshToken = jwtUtil.generateRefreshToken(userDetails);

        refreshTokenService.deleteRefreshToken(refreshTokenEntity);

        tech.onova.flagd_admin_server.domain.entity.RefreshToken newRefreshTokenEntity =
                refreshTokenService.createRefreshToken(userDetails);

        LoginResponseDTO response = new LoginResponseDTO(
                newAccessToken,
                newRefreshTokenEntity.getId(),
                "Bearer"
        );

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequestDTO request) {
        String refreshTokenId = request.refreshToken();

        tech.onova.flagd_admin_server.domain.entity.RefreshToken refreshTokenEntity =
                refreshTokenService.findByToken(refreshTokenId);

        if (refreshTokenEntity != null) {
            refreshTokenService.deleteRefreshToken(refreshTokenEntity);
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}