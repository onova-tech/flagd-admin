package tech.onova.flagd_admin_server.domain.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tech.onova.flagd_admin_server.testutil.TestDataBuilder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class SourceIdTest {

    @Test
    void shouldCreateSourceIdWithUUID() {
        // Given
        UUID uuid = UUID.randomUUID();
        
        // When
        SourceId sourceId = new SourceId(uuid);
        
        // Then
        assertThat(sourceId.id()).isEqualTo(uuid);
    }

    @Test
    void shouldGenerateRandomSourceId() {
        // When
        SourceId sourceId = new SourceId();
        
        // Then
        assertThat(sourceId.id()).isNotNull();
        assertThat(sourceId.id()).isInstanceOf(UUID.class);
    }

    @Test
    void shouldHaveCorrectEquals() {
        // Given
        UUID uuid = UUID.randomUUID();
        SourceId id1 = new SourceId(uuid);
        SourceId id2 = new SourceId(uuid);
        SourceId id3 = new SourceId(UUID.randomUUID());
        
        // Then
        assertThat(id1).isEqualTo(id2);
        assertThat(id1).isNotEqualTo(id3);
        assertThat(id1).isNotEqualTo(null);
        assertThat(id1).isNotEqualTo("string");
    }

    @Test
    void shouldHaveCorrectHashCode() {
        // Given
        UUID uuid = UUID.randomUUID();
        SourceId id1 = new SourceId(uuid);
        SourceId id2 = new SourceId(uuid);
        
        // Then
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }

    @Test
    void shouldHaveCorrectToString() {
        // Given
        UUID uuid = UUID.randomUUID();
        SourceId sourceId = new SourceId(uuid);
        
        // Then
        assertThat(sourceId.toString()).contains(uuid.toString());
    }
}