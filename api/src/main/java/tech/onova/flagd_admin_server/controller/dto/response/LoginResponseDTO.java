package tech.onova.flagd_admin_server.controller.dto.response;

public record LoginResponseDTO(String accessToken, String refreshToken, String type) {}