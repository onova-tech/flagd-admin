package tech.onova.flagd_admin_server.controller.DTOs;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record FlagConfigRequestDTO(
        @NotBlank(message = "Flag name is required")
        String name,
        
        String description,
        
        @NotBlank(message = "Flag state is required")
        String state,
        
        String defaultVariant,
        
        Map<String, Object> variants,
        
        Object targeting
) {
}
