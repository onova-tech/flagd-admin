package tech.onova.flagd_admin_server.domain.entity;

import org.springframework.util.Assert;

public record SourceUri(String uri) {
    public SourceUri {
        Assert.notNull(uri, "Source uri must not be null");
        String trimmedUri = uri.trim();
        Assert.isTrue(trimmedUri.startsWith("file://") && trimmedUri.length() > 7, "Source uri must start with file://");
    }
}