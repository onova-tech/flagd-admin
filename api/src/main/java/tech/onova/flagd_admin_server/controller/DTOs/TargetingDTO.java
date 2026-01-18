package tech.onova.flagd_admin_server.controller.DTOs;

import java.util.Map;

public record TargetingDTO(
        Map<String, String> targetingKey,
        String rule
) {
}
