package tech.onova.flagd_admin_server.domain.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;

class SourceContentAccessExceptionTest {

    @Test
    void shouldCreateExceptionWithMessage() {
        // Given
        String message = "Cannot access source content";

        // When
        SourceContentAccessException exception = new SourceContentAccessException(message);

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(SourceContentAccessException.ERROR_CODE);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void shouldCreateExceptionWithMessageAndCause() {
        // Given
        String message = "Failed to read source content";
        IOException cause = new IOException("Permission denied");

        // When
        SourceContentAccessException exception = new SourceContentAccessException(message, cause);

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(SourceContentAccessException.ERROR_CODE);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void shouldHaveCorrectErrorCode() {
        // Given
        SourceContentAccessException exception = new SourceContentAccessException("message");

        // Then
        assertThat(exception.getErrorCode()).isEqualTo("SOURCE_CONTENT_ACCESS_ERROR");
    }
}
