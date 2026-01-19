package tech.onova.flagd_admin_server.controller.DTOs;

import java.time.ZonedDateTime;
import java.util.UUID;

public record SourceResponseDTO(UUID id,
                               String name,
                               String description,
                               String uri,
                               boolean enabled,
                               ZonedDateTime creationDateTime,
                               ZonedDateTime lastUpdateDateTime,
                               String lastUpdateUserName) {
}
