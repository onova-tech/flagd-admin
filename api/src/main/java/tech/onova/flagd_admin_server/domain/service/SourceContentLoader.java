package tech.onova.flagd_admin_server.domain.service;

import tech.onova.flagd_admin_server.domain.entity.SourceUri;

public interface SourceContentLoader {
    boolean supports(String uriScheme);
    String loadContent(SourceUri sourceUri);
    void initializeContent(SourceUri sourceUri, String content);
}