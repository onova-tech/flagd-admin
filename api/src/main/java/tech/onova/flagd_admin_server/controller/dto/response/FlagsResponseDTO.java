package tech.onova.flagd_admin_server.controller.dto.response;

import java.util.List;

public record FlagsResponseDTO(
        List<FlagDTO> flags
) {
}
