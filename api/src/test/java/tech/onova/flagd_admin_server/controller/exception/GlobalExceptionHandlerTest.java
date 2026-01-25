package tech.onova.flagd_admin_server.controller.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tech.onova.flagd_admin_server.controller.dto.response.ErrorResponseDTO;
import tech.onova.flagd_admin_server.domain.exception.ContentValidationException;
import tech.onova.flagd_admin_server.domain.exception.DomainException;
import tech.onova.flagd_admin_server.domain.exception.SourceContentAccessException;
import tech.onova.flagd_admin_server.domain.exception.SourceContentNotFoundException;

import java.io.IOException;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void shouldHandleDomainException() {
        // Given
        String errorCode = "SOURCE_CONTENT_NOT_FOUND";
        String message = "Source content not found";
        HttpStatus httpStatus = HttpStatus.NOT_FOUND;
        SourceContentNotFoundException domainException = new SourceContentNotFoundException(message);
        
        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleDomainException(domainException);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(httpStatus);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo(errorCode);
        assertThat(response.getBody().message()).isEqualTo(message);
        assertThat(response.getBody().timestamp()).isBeforeOrEqualTo(ZonedDateTime.now());
    }

    @Test
    void shouldHandleContentValidationException() {
        // Given
        String errorCode = ContentValidationException.ERROR_CODE;
        String message = "Invalid flag configuration";
        ContentValidationException validationException = new ContentValidationException(message);
        
        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleDomainException(validationException);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo(errorCode);
        assertThat(response.getBody().message()).isEqualTo(message);
        assertThat(response.getBody().timestamp()).isBeforeOrEqualTo(ZonedDateTime.now());
    }

    @Test
    void shouldHandleSourceContentAccessException() {
        // Given
        String errorCode = "SOURCE_CONTENT_ACCESS_ERROR";
        String message = "Access denied";
        SourceContentAccessException accessException = new SourceContentAccessException(message);
        
        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleDomainException(accessException);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo(errorCode);
        assertThat(response.getBody().message()).isEqualTo(message);
        assertThat(response.getBody().timestamp()).isBeforeOrEqualTo(ZonedDateTime.now());
    }

    @Test
    void shouldHandleDomainExceptionWithCause() {
        // Given
        String message = "Domain error with cause";
        Throwable cause = new RuntimeException("Root cause");
        SourceContentAccessException domainException = new SourceContentAccessException(message, cause);
        
        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleDomainException(domainException);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("SOURCE_CONTENT_ACCESS_ERROR");
        assertThat(response.getBody().message()).isEqualTo(message);
        assertThat(response.getBody().timestamp()).isBeforeOrEqualTo(ZonedDateTime.now());
    }

    @Test
    void shouldHandleIllegalArgumentException() {
        // Given
        String message = "Invalid argument provided";
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException(message);
        
        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleIllegalArgumentException(illegalArgumentException);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("INVALID_ARGUMENT");
        assertThat(response.getBody().message()).isEqualTo(message);
        assertThat(response.getBody().timestamp()).isBeforeOrEqualTo(ZonedDateTime.now());
    }

    @Test
    void shouldHandleIllegalArgumentExceptionWithNullMessage() {
        // Given
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException();
        
        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleIllegalArgumentException(illegalArgumentException);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("INVALID_ARGUMENT");
        // Message can be null when exception has no message
        assertThat(response.getBody().timestamp()).isBeforeOrEqualTo(ZonedDateTime.now());
    }

    @Test
    void shouldHandleGenericException() {
        // Given
        String message = "Unexpected error occurred";
        Exception genericException = new RuntimeException(message);
        
        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleGenericException(genericException);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred: " + message);
        assertThat(response.getBody().timestamp()).isBeforeOrEqualTo(ZonedDateTime.now());
    }

    @Test
    void shouldHandleGenericExceptionWithNullMessage() {
        // Given
        Exception genericException = new RuntimeException();
        
        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleGenericException(genericException);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.getBody().message()).isNotNull();
        assertThat(response.getBody().timestamp()).isBeforeOrEqualTo(ZonedDateTime.now());
    }

    @Test
    void shouldHandleGenericExceptionWithLongMessage() {
        // Given
        String longMessage = "This is a very long error message that might need to be truncated or handled properly in the response to ensure it doesn't break the API response format or exceed any limits";
        Exception genericException = new RuntimeException(longMessage);
        
        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleGenericException(genericException);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred: " + longMessage);
        assertThat(response.getBody().timestamp()).isBeforeOrEqualTo(ZonedDateTime.now());
    }

    @Test
    void shouldHandleGenericRuntimeException() {
        // Given
        RuntimeException genericException = new RuntimeException("Generic runtime error");
        
        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleGenericException(genericException);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.getBody().message()).contains("An unexpected error occurred:");
        assertThat(response.getBody().timestamp()).isBeforeOrEqualTo(ZonedDateTime.now());
    }

    @Test
    void shouldHandleExceptionChains() {
        // Given
        RuntimeException rootCause = new RuntimeException("Root cause");
        IOException intermediateCause = new IOException("Intermediate cause");
        intermediateCause.initCause(rootCause);
        RuntimeException topException = new RuntimeException("Top level exception");
        topException.initCause(intermediateCause);
        
        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleGenericException(topException);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.getBody().message()).contains("Top level exception");
        assertThat(response.getBody().timestamp()).isBeforeOrEqualTo(ZonedDateTime.now());
    }

    @Test
    void shouldCreateErrorResponseWithCorrectTimestamp() {
        // Given
        ZonedDateTime before = ZonedDateTime.now();
        IllegalArgumentException exception = new IllegalArgumentException("Test message");
        
        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleIllegalArgumentException(exception);
        ZonedDateTime after = ZonedDateTime.now();
        
        // Then
        assertThat(response.getBody().timestamp()).isBetween(before, after);
    }

    @Test
    void shouldHandleExceptionWithSpecialCharacters() {
        // Given
        String message = "Error with special chars: ñáéíóú 🚀";
        Exception genericException = new RuntimeException(message);
        
        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleGenericException(genericException);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.getBody().message()).contains(message);
        assertThat(response.getBody().timestamp()).isBeforeOrEqualTo(ZonedDateTime.now());
    }
}