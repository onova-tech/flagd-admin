package tech.onova.flagd_admin_server.domain.service;

import tech.onova.flagd_admin_server.domain.exception.ContentValidationException;

public interface ContentValidator {
    void validateContent(String content) throws ContentValidationException;
}