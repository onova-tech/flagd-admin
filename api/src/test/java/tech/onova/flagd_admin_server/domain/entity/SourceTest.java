package tech.onova.flagd_admin_server.domain.entity;

import org.junit.jupiter.api.Test;
import tech.onova.flagd_admin_server.testutil.TestDataBuilder;

import java.time.LocalDateTime;
import java.time.Duration;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.*;

class SourceTest {

    @Test
    void shouldCreateSourceWithValidData() {
        // Given
        String name = "Test Source";
        String description = "Test Description";
        SourceUri uri = TestDataBuilder.aSourceUri();
        String lastUpdateUser = "test-user";
        boolean enabled = true;

        // When
        Source source = new Source(name, description, uri, lastUpdateUser, enabled);

        // Then
        assertThat(source.getName()).isEqualTo(name);
        assertThat(source.getDescription()).isEqualTo(description);
        assertThat(source.getUri()).isEqualTo(uri);
        assertThat(source.getLastUpdateUserName()).isEqualTo(lastUpdateUser);
        assertThat(source.isEnabled()).isEqualTo(enabled);
        assertThat(source.getId()).isNotNull();
        assertThat(source.getCreationDateTime()).isNotNull();
        assertThat(source.getLastUpdateDateTime()).isNotNull();
        assertThat(source.getCreationDateTime()).isEqualToIgnoringNanos(source.getLastUpdateDateTime());
    }

    @Test
    void shouldRejectEmptyName() {
        // Given
        SourceUri uri = TestDataBuilder.aSourceUri();

        // When & Then
        assertThatThrownBy(() -> new Source("", "description", uri, "user", true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("source name must not be empty");

        assertThatThrownBy(() -> new Source(null, "description", uri, "user", true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("source name must not be empty");
    }

    @Test
    void shouldRejectEmptyDescription() {
        // Given
        SourceUri uri = TestDataBuilder.aSourceUri();

        // When & Then
        assertThatThrownBy(() -> new Source("name", "", uri, "user", true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("source description must not be empty");

        assertThatThrownBy(() -> new Source("name", null, uri, "user", true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("source description must not be empty");
    }

    @Test
    void shouldRejectNullUri() {
        // When & Then
        assertThatThrownBy(() -> new Source("name", "description", null, "user", true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("source uri must not be null");
    }

    @Test
    void shouldRejectEmptyLastUpdateUser() {
        // Given
        SourceUri uri = TestDataBuilder.aSourceUri();

        // When & Then
        assertThatThrownBy(() -> new Source("name", "description", uri, "", true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("last update user must not be empty");

        assertThatThrownBy(() -> new Source("name", "description", uri, null, true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("last update user must not be empty");
    }

    @Test
    void shouldUpdateSourceWithoutChangingUri() {
        // Given
        Source originalSource = TestDataBuilder.aSource()
            .withName("Original Name")
            .withDescription("Original Description")
            .withUri("file://original/path")
            .withEnabled(true)
            .build();

        // When
        Source updatedSource = originalSource.updateWithoutUri(
            "Updated Name",
            "Updated Description",
            false,
            "update-user"
        );

        // Then
        assertThat(updatedSource.getName()).isEqualTo("Updated Name");
        assertThat(updatedSource.getDescription()).isEqualTo("Updated Description");
        // URI should remain unchanged - this is the key security feature
        assertThat(updatedSource.getUri().uri()).isEqualTo("file://original/path");
        assertThat(updatedSource.isEnabled()).isEqualTo(false);
        assertThat(updatedSource.getLastUpdateUserName()).isEqualTo("update-user");
        
        // ID and creation date should remain unchanged
        assertThat(updatedSource.getId()).isEqualTo(originalSource.getId());
        assertThat(updatedSource.getCreationDateTime()).isEqualTo(originalSource.getCreationDateTime());
        
        // Last update date should be updated
        assertThat(updatedSource.getLastUpdateDateTime()).isNotNull();
        assertThat(updatedSource.getLastUpdateDateTime()).isBeforeOrEqualTo(ZonedDateTime.now().plusSeconds(1));
    }

    @Test
    void shouldHaveCorrectEquals() {
        // Given
        SourceId sourceId = TestDataBuilder.aSourceId();
        Source source1 = TestDataBuilder.aSource().withId(sourceId.id()).build();
        Source source2 = TestDataBuilder.aSource().withId(sourceId.id()).build();
        Source source3 = TestDataBuilder.aSource().build();

        // Then
        assertThat(source1).isEqualTo(source2);
        assertThat(source1).isNotEqualTo(source3);
        assertThat(source1).isNotEqualTo(null);
        assertThat(source1).isNotEqualTo("string");
    }

    @Test
    void shouldHaveCorrectHashCode() {
        // Given
        SourceId sourceId = TestDataBuilder.aSourceId();
        Source source1 = TestDataBuilder.aSource().withId(sourceId.id()).build();
        Source source2 = TestDataBuilder.aSource().withId(sourceId.id()).build();

        // Then
        assertThat(source1.hashCode()).isEqualTo(source2.hashCode());
    }

    @Test
    void shouldHaveCorrectToString() {
        // Given
        Source source = TestDataBuilder.aSource()
            .withName("Test Source")
            .build();

        // Then
        assertThat(source.toString()).contains("Test Source");
    }
}