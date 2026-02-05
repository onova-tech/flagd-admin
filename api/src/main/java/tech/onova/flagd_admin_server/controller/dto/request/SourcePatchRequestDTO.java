package tech.onova.flagd_admin_server.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SourcePatchRequestDTO(@NotBlank(message="Source name is required")
                                    String name,
                                    @NotBlank(message="Source description is required")
                                    String description,
                                    boolean enabled) {
}
