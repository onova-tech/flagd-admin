package tech.onova.flagd_admin_server.domain.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;

class DomainExceptionTest {

    @Test
    void shouldCreateExceptionWithErrorCodeAndMessage() {
        // Given
        String errorCode = "TEST_ERROR";
        String message = "Test error message";
        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;

        // When
        TestDomainException exception = new TestDomainException(errorCode, message, httpStatus);

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getHttpStatus()).isEqualTo(httpStatus);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateExceptionWithErrorCodeMessageAndCause() {
        // Given
        String errorCode = "TEST_ERROR";
        String message = "Test error message";
        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        IOException cause = new IOException("Root cause");

        // When
        TestDomainException exception = new TestDomainException(errorCode, message, httpStatus, cause);

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getHttpStatus()).isEqualTo(httpStatus);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void shouldBeRuntimeException() {
        // Given & When
        TestDomainException exception = new TestDomainException("CODE", "message", HttpStatus.BAD_REQUEST);

        // Then
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    private static class TestDomainException extends DomainException {
        public TestDomainException(String errorCode, String message, HttpStatus httpStatus) {
            super(errorCode, message, httpStatus);
        }

        public TestDomainException(String errorCode, String message, HttpStatus httpStatus, Throwable cause) {
            super(errorCode, message, httpStatus, cause);
        }
    }
}
