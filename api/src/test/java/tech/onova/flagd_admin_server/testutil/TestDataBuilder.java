package tech.onova.flagd_admin_server.testutil;

import tech.onova.flagd_admin_server.domain.entity.Source;
import tech.onova.flagd_admin_server.domain.entity.SourceId;
import tech.onova.flagd_admin_server.domain.entity.SourceUri;

import java.time.LocalDateTime;
import java.util.UUID;

public class TestDataBuilder {

    public static class SourceBuilder {
        private UUID id = UUID.randomUUID();
        private String name = "Test Source";
        private String description = "Test Description";
        private String uri = "file://test/path";
        private String lastUpdateUser = "test-user";
        private boolean enabled = true;
        private LocalDateTime creationDateTime = LocalDateTime.now();
        private LocalDateTime lastUpdateDateTime = LocalDateTime.now();

        public SourceBuilder withId(UUID id) {
            this.id = id;
            return this;
        }

        public SourceBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public SourceBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public SourceBuilder withUri(String uri) {
            this.uri = uri;
            return this;
        }

        public SourceBuilder withEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public SourceBuilder withLastUpdateUser(String lastUpdateUser) {
            this.lastUpdateUser = lastUpdateUser;
            return this;
        }

        public Source build() {
            Source source = new Source(name, description, new SourceUri(uri), lastUpdateUser, enabled);
            
            // Use reflection to set ID field for testing (keep default timestamps)
            try {
                var idField = Source.class.getDeclaredField("id");
                idField.setAccessible(true);
                var sourceId = new SourceId(id);
                idField.set(source, sourceId);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set source ID field via reflection", e);
            }
            
            return source;
        }
    }

    public static SourceBuilder aSource() {
        return new SourceBuilder();
    }

    public static SourceId aSourceId() {
        return new SourceId(UUID.randomUUID());
    }

    public static SourceUri aSourceUri() {
        return new SourceUri("file://test/path");
    }

    public static SourceUri aSourceUri(String path) {
        return new SourceUri("file://" + path);
    }
}