package tech.onova.flagd_admin_server.domain.exception;

import org.springframework.http.HttpStatus;

public abstract class DomainException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus httpStatus;

    public DomainException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public DomainException(String errorCode, String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}