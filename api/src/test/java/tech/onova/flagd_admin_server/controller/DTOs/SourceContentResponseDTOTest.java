package tech.onova.flagd_admin_server.controller.DTOs;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class SourceContentResponseDTOTest {

    @Test
    void shouldCreateSourceContentResponseDTO() {
        // Given
        String content = "{\"flags\": {}}";

        // When
        SourceContentResponseDTO dto = new SourceContentResponseDTO(content);

        // Then
        assertThat(dto.content()).isEqualTo(content);
    }

    @Test
    void shouldHandleEmptyContent() {
        // Given
        SourceContentResponseDTO dto = new SourceContentResponseDTO("");

        // Then
        assertThat(dto.content()).isEmpty();
    }

    @Test
    void shouldHandleNullContent() {
        // Given & When
        SourceContentResponseDTO dto = new SourceContentResponseDTO(null);

        // Then
        assertThat(dto.content()).isNull();
    }

    @Test
    void shouldHandleComplexJsonContent() {
        // Given
        String complexContent = """
            {
              "$schema": "https://flagd.dev/schema/v0/flags.json",
              "flags": {
                "flag1": {
                  "key": "flag1",
                  "state": "ENABLED"
                }
              }
            }
            """;

        // When
        SourceContentResponseDTO dto = new SourceContentResponseDTO(complexContent);

        // Then
        assertThat(dto.content()).isEqualTo(complexContent);
        assertThat(dto.content()).contains("$schema");
        assertThat(dto.content()).contains("flag1");
    }

    @Test
    void shouldBeImmutableRecord() {
        // Given
        SourceContentResponseDTO dto = new SourceContentResponseDTO("content");

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getClass().isRecord()).isTrue();
    }
}
