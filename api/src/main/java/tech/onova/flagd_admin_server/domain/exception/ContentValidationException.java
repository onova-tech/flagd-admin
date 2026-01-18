package tech.onova.flagd_admin_server.domain.exception;

import org.springframework.http.HttpStatus;

public class ContentValidationException extends DomainException {
    public static final String ERROR_CODE = "CONTENT_VALIDATION_ERROR";

    public ContentValidationException(String message) {
        super(ERROR_CODE, message, HttpStatus.BAD_REQUEST);
    }

    public ContentValidationException(String message, Throwable cause) {
        super(ERROR_CODE, message, HttpStatus.BAD_REQUEST, cause);
    }
}