package tech.onova.flagd_admin_server.controller.DTOs;

import java.util.List;

public record FlagsResponseDTO(
        List<FlagDTO> flags
) {
}
