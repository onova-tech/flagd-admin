package tech.onova.flagd_admin_server.domain.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.*;

class UnsupportedSourceUriExceptionTest {

    @Test
    void shouldCreateExceptionWithMessage() {
        // Given
        String message = "Unsupported source URI scheme";

        // When
        UnsupportedSourceUriException exception = new UnsupportedSourceUriException(message);

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(UnsupportedSourceUriException.ERROR_CODE);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldCreateExceptionWithMessageAndCause() {
        // Given
        String message = "Invalid URI scheme";
        IllegalArgumentException cause = new IllegalArgumentException("URI must start with file://");

        // When
        UnsupportedSourceUriException exception = new UnsupportedSourceUriException(message, cause);

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(UnsupportedSourceUriException.ERROR_CODE);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldHaveCorrectErrorCode() {
        // Given
        UnsupportedSourceUriException exception = new UnsupportedSourceUriException("message");

        // Then
        assertThat(exception.getErrorCode()).isEqualTo("UNSUPPORTED_SOURCE_URI");
    }
}
