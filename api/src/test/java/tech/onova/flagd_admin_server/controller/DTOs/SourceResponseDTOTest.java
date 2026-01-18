package tech.onova.flagd_admin_server.controller.DTOs;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class SourceResponseDTOTest {

    @Test
    void shouldCreateSourceResponseDTO() {
        // Given
        UUID id = UUID.randomUUID();
        String name = "Test Source";
        String description = "Test Description";
        String uri = "file://test/path";
        boolean enabled = true;
        ZonedDateTime creationDateTime = ZonedDateTime.now();
        ZonedDateTime lastUpdateDateTime = ZonedDateTime.now();
        String lastUpdateUserName = "user";

        // When
        SourceResponseDTO dto = new SourceResponseDTO(
            id, name, description, uri, enabled,
            creationDateTime, lastUpdateDateTime, lastUpdateUserName
        );

        // Then
        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.name()).isEqualTo(name);
        assertThat(dto.description()).isEqualTo(description);
        assertThat(dto.uri()).isEqualTo(uri);
        assertThat(dto.enabled()).isEqualTo(enabled);
        assertThat(dto.creationDateTime()).isEqualTo(creationDateTime);
        assertThat(dto.lastUpdateDateTime()).isEqualTo(lastUpdateDateTime);
        assertThat(dto.lastUpdateUserName()).isEqualTo(lastUpdateUserName);
    }

    @Test
    void shouldBeImmutableRecord() {
        // Given
        SourceResponseDTO dto = new SourceResponseDTO(
            UUID.randomUUID(), "name", "description", "file://path",
            true, ZonedDateTime.now(), ZonedDateTime.now(), "user"
        );

        // Then - Records are immutable by design
        assertThat(dto).isNotNull();
        assertThat(dto.getClass().isRecord()).isTrue();
    }
}
