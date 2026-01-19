package tech.onova.flagd_admin_server.controller.DTOs;

import jakarta.validation.constraints.NotBlank;

public record SourceContentInitRequestDTO(@NotBlank(message="Content is required")
                                      String content) {
}