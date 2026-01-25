package tech.onova.flagd_admin_server.domain.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationException extends DomainException {

    public AuthenticationException(String message) {
        super("AUTHENTICATION_ERROR", message, HttpStatus.UNAUTHORIZED);
    }

    public AuthenticationException(String message, Throwable cause) {
        super("AUTHENTICATION_ERROR", message, HttpStatus.UNAUTHORIZED, cause);
    }
}