package tech.onova.flagd_admin_server.domain.exception;

import org.springframework.http.HttpStatus;

public class UnsupportedSourceUriException extends DomainException {
    public static final String ERROR_CODE = "UNSUPPORTED_SOURCE_URI";

    public UnsupportedSourceUriException(String message) {
        super(ERROR_CODE, message, HttpStatus.BAD_REQUEST);
    }

    public UnsupportedSourceUriException(String message, Throwable cause) {
        super(ERROR_CODE, message, HttpStatus.BAD_REQUEST, cause);
    }
}