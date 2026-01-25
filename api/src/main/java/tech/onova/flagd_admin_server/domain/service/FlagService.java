package tech.onova.flagd_admin_server.domain.service;

import tech.onova.flagd_admin_server.controller.dto.request.FlagConfigRequestDTO;
import tech.onova.flagd_admin_server.controller.dto.response.FlagDTO;
import tech.onova.flagd_admin_server.domain.entity.SourceId;
import tech.onova.flagd_admin_server.infrastructure.annotation.Log;
import java.util.List;
import java.util.Map;

public interface FlagService {
    
    @Log
    List<FlagDTO> getFlags(SourceId sourceId);
    
    @Log
    FlagDTO getFlag(SourceId sourceId, String flagId);
    
    @Log
    void addOrUpdateFlag(SourceId sourceId, String flagId, FlagConfigRequestDTO request);
    
    @Log
    void deleteFlag(SourceId sourceId, String flagId);
}
