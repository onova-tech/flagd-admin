package tech.onova.flagd_admin_server.controller.DTOs;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class FlagDTOTest {

    @Test
    void shouldCreateFlagDTOWithAllFields() {
        // Given
        String flagId = "my-flag";
        String name = "My Flag";
        String description = "A test flag";
        String state = "ENABLED";
        String defaultVariant = "on";
        Map<String, Object> variants = Map.of("on", true, "off", false);
        TargetingDTO targeting = new TargetingDTO(Map.of("userId", "string"), "if (userId == 'test') return true");

        // When
        FlagDTO dto = new FlagDTO(flagId, name, description, state, defaultVariant, variants, targeting);

        // Then
        assertThat(dto.flagId()).isEqualTo(flagId);
        assertThat(dto.name()).isEqualTo(name);
        assertThat(dto.description()).isEqualTo(description);
        assertThat(dto.state()).isEqualTo(state);
        assertThat(dto.defaultVariant()).isEqualTo(defaultVariant);
        assertThat(dto.variants()).isEqualTo(variants);
        assertThat(dto.targeting()).isEqualTo(targeting);
    }

    @Test
    void shouldCreateFlagDTOWithMinimalFields() {
        // Given
        FlagDTO dto = new FlagDTO("flag-id", null, null, null, null, null, null);

        // Then
        assertThat(dto.flagId()).isEqualTo("flag-id");
        assertThat(dto.name()).isNull();
        assertThat(dto.description()).isNull();
        assertThat(dto.state()).isNull();
        assertThat(dto.defaultVariant()).isNull();
        assertThat(dto.variants()).isNull();
        assertThat(dto.targeting()).isNull();
    }

    @Test
    void shouldCreateFlagDTOWithVariants() {
        // Given
        Map<String, Object> variants = Map.of(
            "on", true,
            "off", false,
            "maybe", 42
        );

        // When
        FlagDTO dto = new FlagDTO("flag", "Name", "Desc", "ENABLED", "on", variants, null);

        // Then
        assertThat(dto.variants()).isNotNull();
        assertThat(dto.variants()).hasSize(3);
        assertThat(dto.variants()).containsEntry("on", true);
        assertThat(dto.variants()).containsEntry("maybe", 42);
    }

    @Test
    void shouldBeImmutableRecord() {
        // Given
        FlagDTO dto = new FlagDTO("flag", "Name", "Desc", "ENABLED", "on", Map.of(), null);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getClass().isRecord()).isTrue();
    }
}
