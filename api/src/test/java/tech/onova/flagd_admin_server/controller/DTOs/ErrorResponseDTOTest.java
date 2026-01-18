package tech.onova.flagd_admin_server.controller.DTOs;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.*;

class ErrorResponseDTOTest {

    @Test
    void shouldCreateErrorResponseDTO() {
        // Given
        String errorCode = "ERROR_CODE";
        String message = "Error message";
        ZonedDateTime timestamp = ZonedDateTime.now();

        // When
        ErrorResponseDTO dto = new ErrorResponseDTO(errorCode, message, timestamp);

        // Then
        assertThat(dto.errorCode()).isEqualTo(errorCode);
        assertThat(dto.message()).isEqualTo(message);
        assertThat(dto.timestamp()).isEqualTo(timestamp);
    }

    @Test
    void shouldAcceptNullTimestamp() {
        // When & Then
        assertThatCode(() -> new ErrorResponseDTO("ERROR", "message", null))
            .doesNotThrowAnyException();
    }

    @Test
    void shouldHandleEmptyErrorCode() {
        // Given
        ErrorResponseDTO dto = new ErrorResponseDTO("", "message", ZonedDateTime.now());

        // Then
        assertThat(dto.errorCode()).isEmpty();
    }

    @Test
    void shouldBeImmutableRecord() {
        // Given
        ErrorResponseDTO dto = new ErrorResponseDTO("ERROR", "message", ZonedDateTime.now());

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getClass().isRecord()).isTrue();
    }
}
