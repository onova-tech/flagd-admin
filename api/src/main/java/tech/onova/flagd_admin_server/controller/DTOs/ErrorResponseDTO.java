package tech.onova.flagd_admin_server.controller.DTOs;

import java.time.ZonedDateTime;

public record ErrorResponseDTO(
        String errorCode,
        String message,
        ZonedDateTime timestamp
) {
}