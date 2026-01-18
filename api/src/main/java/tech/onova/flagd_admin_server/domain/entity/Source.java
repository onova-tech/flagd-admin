package tech.onova.flagd_admin_server.domain.entity;

import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import org.springframework.util.Assert;

import java.time.ZonedDateTime;

@Entity
public class Source {
    @EmbeddedId
    private SourceId id;
    private String name;
    private String description;
    @Embedded
    private SourceUri uri;
    private boolean enabled;
    private ZonedDateTime creationDateTime;
    private ZonedDateTime lastUpdateDateTime;
    private String lastUpdateUserName;

    protected Source(){}

    public Source(String name, String description, SourceUri uri, String userName, boolean enabled){
        Assert.hasText(name, "source name must not be empty");
        Assert.hasText(description, "source description must not be empty");
        Assert.notNull(uri, "source uri must not be null");
        Assert.hasText(userName, "last update user must not be empty");

        this.id = new SourceId();
        this.name = name;
        this.description = description;
        this.uri = uri;
        this.creationDateTime = ZonedDateTime.now();
        this.enabled = enabled;
        this.lastUpdateDateTime = ZonedDateTime.now();
        this.lastUpdateUserName = userName;
    }

    public Source update(Source source) {
        this.name = source.getName();
        this.description = source.getDescription();
        this.uri = source.getUri();
        this.enabled = source.isEnabled();
        this.lastUpdateDateTime = ZonedDateTime.now();
        this.lastUpdateUserName = source.getLastUpdateUserName();

        return this;
    }

    public SourceId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public SourceUri getUri() {
        return uri;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public ZonedDateTime getCreationDateTime() {
        return creationDateTime;
    }

    public ZonedDateTime getLastUpdateDateTime() {
        return lastUpdateDateTime;
    }

    public String getLastUpdateUserName() {
        return lastUpdateUserName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Source source = (Source) o;
        return id.equals(source.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Source{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", uri='" + uri + '\'' +
                ", enabled=" + enabled +
                ", creationDateTime=" + creationDateTime +
                ", lastUpdateDateTime=" + lastUpdateDateTime +
                ", lastUpdateUserName='" + lastUpdateUserName + '\'' +
                '}';
    }
}
