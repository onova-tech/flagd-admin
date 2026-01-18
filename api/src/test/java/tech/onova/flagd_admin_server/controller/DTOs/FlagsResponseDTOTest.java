package tech.onova.flagd_admin_server.controller.DTOs;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class FlagsResponseDTOTest {

    @Test
    void shouldCreateFlagsResponseDTO() {
        // Given
        List<FlagDTO> flags = List.of(
            new FlagDTO("flag1", "Flag 1", "Description 1", "ENABLED", "on", Map.of("on", true), null),
            new FlagDTO("flag2", "Flag 2", "Description 2", "DISABLED", "off", Map.of("off", false), null)
        );

        // When
        FlagsResponseDTO dto = new FlagsResponseDTO(flags);

        // Then
        assertThat(dto.flags()).isNotNull();
        assertThat(dto.flags()).hasSize(2);
        assertThat(dto.flags().get(0).flagId()).isEqualTo("flag1");
        assertThat(dto.flags().get(1).flagId()).isEqualTo("flag2");
    }

    @Test
    void shouldCreateFlagsResponseDTOWithEmptyList() {
        // Given
        List<FlagDTO> flags = List.of();

        // When
        FlagsResponseDTO dto = new FlagsResponseDTO(flags);

        // Then
        assertThat(dto.flags()).isNotNull();
        assertThat(dto.flags()).isEmpty();
    }

    @Test
    void shouldCreateFlagsResponseDTOWithNullList() {
        // Given & When
        FlagsResponseDTO dto = new FlagsResponseDTO(null);

        // Then
        assertThat(dto.flags()).isNull();
    }

    @Test
    void shouldBeImmutableRecord() {
        // Given
        FlagsResponseDTO dto = new FlagsResponseDTO(List.of());

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getClass().isRecord()).isTrue();
    }
}
