package tech.onova.flagd_admin_server.domain.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.*;

class SourceNotFoundExceptionTest {

    @Test
    void shouldCreateExceptionWithMessage() {
        // Given
        String message = "Source not found";

        // When
        SourceNotFoundException exception = new SourceNotFoundException(message);

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(SourceNotFoundException.ERROR_CODE);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldCreateExceptionWithMessageAndCause() {
        // Given
        String message = "Failed to find source";
        RuntimeException cause = new RuntimeException("Database error");

        // When
        SourceNotFoundException exception = new SourceNotFoundException(message, cause);

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(SourceNotFoundException.ERROR_CODE);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldHaveCorrectErrorCode() {
        // Given
        SourceNotFoundException exception = new SourceNotFoundException("message");

        // Then
        assertThat(exception.getErrorCode()).isEqualTo("SOURCE_NOT_FOUND");
    }
}
