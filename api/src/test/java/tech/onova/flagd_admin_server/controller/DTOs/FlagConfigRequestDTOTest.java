package tech.onova.flagd_admin_server.controller.DTOs;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class FlagConfigRequestDTOTest {

    @Test
    void shouldCreateFlagConfigRequestDTOWithAllFields() {
        // Given
        String name = "My Flag";
        String description = "A test flag";
        String state = "ENABLED";
        String defaultVariant = "on";
        Map<String, Object> variants = Map.of("on", true, "off", false);
        TargetingDTO targeting = new TargetingDTO(Map.of("userId", "string"), "if (userId == 'test') return true");

        // When
        FlagConfigRequestDTO dto = new FlagConfigRequestDTO(name, description, state, defaultVariant, variants, targeting);

        // Then
        assertThat(dto.name()).isEqualTo(name);
        assertThat(dto.description()).isEqualTo(description);
        assertThat(dto.state()).isEqualTo(state);
        assertThat(dto.defaultVariant()).isEqualTo(defaultVariant);
        assertThat(dto.variants()).isEqualTo(variants);
        assertThat(dto.targeting()).isEqualTo(targeting);
    }

    @Test
    void shouldCreateFlagConfigRequestDTOWithMinimalFields() {
        // Given
        FlagConfigRequestDTO dto = new FlagConfigRequestDTO("flag-name", null, "ENABLED", null, null, null);

        // Then
        assertThat(dto.name()).isEqualTo("flag-name");
        assertThat(dto.description()).isNull();
        assertThat(dto.state()).isEqualTo("ENABLED");
        assertThat(dto.defaultVariant()).isNull();
        assertThat(dto.variants()).isNull();
        assertThat(dto.targeting()).isNull();
    }

    @Test
    void shouldAcceptVariants() {
        // Given
        Map<String, Object> variants = Map.of(
            "on", true,
            "off", false,
            "maybe", 42
        );

        // When
        FlagConfigRequestDTO dto = new FlagConfigRequestDTO("flag", "Desc", "ENABLED", "on", variants, null);

        // Then
        assertThat(dto.variants()).isNotNull();
        assertThat(dto.variants()).hasSize(3);
    }

    @Test
    void shouldBeImmutableRecord() {
        // Given
        FlagConfigRequestDTO dto = new FlagConfigRequestDTO("flag", "Desc", "ENABLED", "on", Map.of(), null);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getClass().isRecord()).isTrue();
    }
}
