package tech.onova.flagd_admin_server.controller.dto.response;

import java.util.Map;

public record FlagDTO(
        String flagId,
        String name,
        String description,
        String state,
        String defaultVariant,
        Map<String, Object> variants,
        Object targeting
) {
}
