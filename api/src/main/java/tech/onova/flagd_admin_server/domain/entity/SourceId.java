package tech.onova.flagd_admin_server.domain.entity;

import org.springframework.util.Assert;

import java.util.UUID;

public record SourceId(UUID id) {
    public SourceId {
        Assert.notNull(id, "source id must not be null");
    }

    public SourceId() {
        this(UUID.randomUUID());
    }
}