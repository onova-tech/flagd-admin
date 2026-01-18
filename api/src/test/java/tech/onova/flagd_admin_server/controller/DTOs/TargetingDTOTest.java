package tech.onova.flagd_admin_server.controller.DTOs;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class TargetingDTOTest {

    @Test
    void shouldCreateTargetingDTOWithAllFields() {
        // Given
        Map<String, String> targetingKey = Map.of("userId", "string", "email", "string");
        String rule = "if (userId in ['user1', 'user2']) return true";

        // When
        TargetingDTO dto = new TargetingDTO(targetingKey, rule);

        // Then
        assertThat(dto.targetingKey()).isEqualTo(targetingKey);
        assertThat(dto.rule()).isEqualTo(rule);
    }

    @Test
    void shouldCreateTargetingDTOWithNullFields() {
        // Given
        TargetingDTO dto = new TargetingDTO(null, null);

        // Then
        assertThat(dto.targetingKey()).isNull();
        assertThat(dto.rule()).isNull();
    }

    @Test
    void shouldCreateTargetingDTOWithOnlyRule() {
        // Given
        TargetingDTO dto = new TargetingDTO(null, "return true");

        // Then
        assertThat(dto.targetingKey()).isNull();
        assertThat(dto.rule()).isEqualTo("return true");
    }

    @Test
    void shouldCreateTargetingDTOWithOnlyTargetingKey() {
        // Given
        Map<String, String> targetingKey = Map.of("userId", "string");
        TargetingDTO dto = new TargetingDTO(targetingKey, null);

        // Then
        assertThat(dto.targetingKey()).isEqualTo(targetingKey);
        assertThat(dto.rule()).isNull();
    }

    @Test
    void shouldBeImmutableRecord() {
        // Given
        TargetingDTO dto = new TargetingDTO(Map.of(), "rule");

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getClass().isRecord()).isTrue();
    }
}
