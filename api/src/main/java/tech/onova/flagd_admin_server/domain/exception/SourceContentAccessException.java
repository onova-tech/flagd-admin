package tech.onova.flagd_admin_server.domain.exception;

import org.springframework.http.HttpStatus;

public class SourceContentAccessException extends DomainException {
    public static final String ERROR_CODE = "SOURCE_CONTENT_ACCESS_ERROR";

    public SourceContentAccessException(String message) {
        super(ERROR_CODE, message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public SourceContentAccessException(String message, Throwable cause) {
        super(ERROR_CODE, message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}