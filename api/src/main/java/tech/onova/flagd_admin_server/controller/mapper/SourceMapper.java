package tech.onova.flagd_admin_server.controller.mapper;

import org.springframework.stereotype.Component;
import tech.onova.flagd_admin_server.controller.dto.response.SourceResponseDTO;
import tech.onova.flagd_admin_server.domain.entity.Source;

/**
 * Mapper service for converting between Source entities and DTOs
 */
@Component
public class SourceMapper {

    /**
     * Convert Source entity to SourceResponseDTO
     */
    public SourceResponseDTO toResponseDTO(Source source) {
        return new SourceResponseDTO(
                source.getId().id(),
                source.getName(),
                source.getDescription(),
                source.getUri().uri(),
                source.isEnabled(),
                source.getCreationDateTime(),
                source.getLastUpdateDateTime(),
                source.getLastUpdateUserName()
        );
    }
}