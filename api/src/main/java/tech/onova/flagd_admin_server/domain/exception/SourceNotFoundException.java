package tech.onova.flagd_admin_server.domain.exception;

import org.springframework.http.HttpStatus;

public class SourceNotFoundException extends DomainException {
    public static final String ERROR_CODE = "SOURCE_NOT_FOUND";

    public SourceNotFoundException(String message) {
        super(ERROR_CODE, message, HttpStatus.NOT_FOUND);
    }

    public SourceNotFoundException(String message, Throwable cause) {
        super(ERROR_CODE, message, HttpStatus.NOT_FOUND, cause);
    }
}