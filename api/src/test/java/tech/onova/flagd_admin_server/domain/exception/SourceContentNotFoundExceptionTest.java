package tech.onova.flagd_admin_server.domain.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;

class SourceContentNotFoundExceptionTest {

    @Test
    void shouldCreateExceptionWithMessage() {
        // Given
        String message = "Source content file not found";

        // When
        SourceContentNotFoundException exception = new SourceContentNotFoundException(message);

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(SourceContentNotFoundException.ERROR_CODE);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldCreateExceptionWithMessageAndCause() {
        // Given
        String message = "Failed to load source content";
        IOException cause = new IOException("File does not exist");

        // When
        SourceContentNotFoundException exception = new SourceContentNotFoundException(message, cause);

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(SourceContentNotFoundException.ERROR_CODE);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldHaveCorrectErrorCode() {
        // Given
        SourceContentNotFoundException exception = new SourceContentNotFoundException("message");

        // Then
        assertThat(exception.getErrorCode()).isEqualTo("SOURCE_CONTENT_NOT_FOUND");
    }
}
