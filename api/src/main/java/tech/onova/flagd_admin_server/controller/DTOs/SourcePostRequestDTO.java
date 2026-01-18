package tech.onova.flagd_admin_server.controller.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SourcePostRequestDTO(@NotBlank(message="Source name is required")
                               String name,
                                   @NotBlank(message="Source description is required")
                               String description,
                                   @Pattern(regexp = "^(file://).*$",
                                 message="Source uri must start with file://")
                               String uri) {
}
