package tech.onova.flagd_admin_server.domain.exception;

import org.springframework.http.HttpStatus;

public class SourceContentNotFoundException extends DomainException {
    public static final String ERROR_CODE = "SOURCE_CONTENT_NOT_FOUND";

    public SourceContentNotFoundException(String message) {
        super(ERROR_CODE, message, HttpStatus.NOT_FOUND);
    }

    public SourceContentNotFoundException(String message, Throwable cause) {
        super(ERROR_CODE, message, HttpStatus.NOT_FOUND, cause);
    }
}