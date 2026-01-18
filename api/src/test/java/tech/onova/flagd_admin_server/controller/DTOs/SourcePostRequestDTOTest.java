package tech.onova.flagd_admin_server.controller.DTOs;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class SourcePostRequestDTOTest {

    @Test
    void shouldCreateSourcePostRequestDTO() {
        // Given
        String name = "Test Source";
        String description = "Test Description";
        String uri = "file://test/path";

        // When
        SourcePostRequestDTO dto = new SourcePostRequestDTO(name, description, uri);

        // Then
        assertThat(dto.name()).isEqualTo(name);
        assertThat(dto.description()).isEqualTo(description);
        assertThat(dto.uri()).isEqualTo(uri);
    }

    @Test
    void shouldAcceptValidUri() {
        // Given
        String validUri = "file:///absolute/path/to/file.json";

        // When & Then
        assertThatCode(() -> new SourcePostRequestDTO("name", "description", validUri))
            .doesNotThrowAnyException();
    }

    @Test
    void shouldAcceptRelativeUri() {
        // Given
        String relativeUri = "file://relative/path/to/file.json";

        // When & Then
        assertThatCode(() -> new SourcePostRequestDTO("name", "description", relativeUri))
            .doesNotThrowAnyException();
    }

    @Test
    void shouldBeImmutableRecord() {
        // Given
        SourcePostRequestDTO dto = new SourcePostRequestDTO("name", "description", "file://path");

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getClass().isRecord()).isTrue();
    }
}
