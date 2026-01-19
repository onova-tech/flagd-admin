package tech.onova.flagd_admin_server.domain.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.*;

class ContentValidationExceptionTest {

    @Test
    void shouldCreateExceptionWithMessage() {
        // Given
        String message = "Invalid flagd configuration";

        // When
        ContentValidationException exception = new ContentValidationException(message);

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ContentValidationException.ERROR_CODE);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldCreateExceptionWithMessageAndCause() {
        // Given
        String message = "Failed to validate content";
        RuntimeException cause = new RuntimeException("Root cause");

        // When
        ContentValidationException exception = new ContentValidationException(message, cause);

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ContentValidationException.ERROR_CODE);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldHaveCorrectErrorCode() {
        // Given
        ContentValidationException exception = new ContentValidationException("message");

        // Then
        assertThat(exception.getErrorCode()).isEqualTo("CONTENT_VALIDATION_ERROR");
    }
}
