package tech.onova.flagd_admin_server.domain.service;

import tech.onova.flagd_admin_server.domain.entity.SourceUri;
import tech.onova.flagd_admin_server.domain.exception.ContentValidationException;
import tech.onova.flagd_admin_server.domain.exception.SourceContentAccessException;
import tech.onova.flagd_admin_server.domain.exception.SourceContentNotFoundException;

public interface SourceContentService {
    
    String loadContent(SourceUri sourceUri) throws SourceContentNotFoundException, SourceContentAccessException;
    
    void initializeContent(SourceUri sourceUri, String content) throws SourceContentAccessException, ContentValidationException;
    
    boolean contentExists(SourceUri sourceUri) throws SourceContentAccessException;
    
    void initializeContentWithConfig(SourceUri sourceUri) throws SourceContentAccessException, ContentValidationException;
}
