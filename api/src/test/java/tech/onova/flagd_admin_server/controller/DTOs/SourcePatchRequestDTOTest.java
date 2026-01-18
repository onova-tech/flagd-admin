package tech.onova.flagd_admin_server.controller.DTOs;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class SourcePatchRequestDTOTest {

    @Test
    void shouldCreateSourcePatchRequestDTO() {
        // Given
        String name = "Updated Source";
        String description = "Updated Description";
        String uri = "file://updated/path";
        boolean enabled = false;

        // When
        SourcePatchRequestDTO dto = new SourcePatchRequestDTO(name, description, uri, enabled);

        // Then
        assertThat(dto.name()).isEqualTo(name);
        assertThat(dto.description()).isEqualTo(description);
        assertThat(dto.uri()).isEqualTo(uri);
        assertThat(dto.enabled()).isEqualTo(enabled);
    }

    @Test
    void shouldAcceptValidUri() {
        // Given
        String validUri = "file:///absolute/path/to/file.json";

        // When & Then
        assertThatCode(() -> new SourcePatchRequestDTO("name", "description", validUri, true))
            .doesNotThrowAnyException();
    }

    @Test
    void shouldHandleDisabledSource() {
        // Given
        SourcePatchRequestDTO dto = new SourcePatchRequestDTO("name", "description", "file://path", false);

        // Then
        assertThat(dto.enabled()).isFalse();
    }

    @Test
    void shouldHandleEnabledSource() {
        // Given
        SourcePatchRequestDTO dto = new SourcePatchRequestDTO("name", "description", "file://path", true);

        // Then
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    void shouldBeImmutableRecord() {
        // Given
        SourcePatchRequestDTO dto = new SourcePatchRequestDTO("name", "description", "file://path", true);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getClass().isRecord()).isTrue();
    }
}
