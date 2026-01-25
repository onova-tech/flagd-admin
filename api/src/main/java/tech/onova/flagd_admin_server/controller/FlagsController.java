package tech.onova.flagd_admin_server.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.onova.flagd_admin_server.controller.dto.request.FlagConfigRequestDTO;
import tech.onova.flagd_admin_server.controller.dto.response.FlagDTO;
import tech.onova.flagd_admin_server.controller.dto.response.FlagsResponseDTO;
import tech.onova.flagd_admin_server.domain.entity.SourceId;
import tech.onova.flagd_admin_server.domain.service.FlagService;
import tech.onova.flagd_admin_server.infrastructure.annotation.Log;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class FlagsController {
    private final FlagService flagService;

    @Autowired
    public FlagsController(FlagService flagService) {
        this.flagService = flagService;
    }

    @GetMapping("/sources/{sourceId}/flags")
    @Log
    public ResponseEntity<FlagsResponseDTO> getFlags(@PathVariable UUID sourceId) {
        List<FlagDTO> flags = flagService.getFlags(new SourceId(sourceId));
        return new ResponseEntity<>(new FlagsResponseDTO(flags), HttpStatus.OK);
    }

    @GetMapping("/sources/{sourceId}/flags/{flagId}")
    @Log
    public ResponseEntity<FlagDTO> getFlag(@PathVariable UUID sourceId, @PathVariable String flagId) {
        FlagDTO flag = flagService.getFlag(new SourceId(sourceId), flagId);
        if (flag == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(flag, HttpStatus.OK);
    }

    @PostMapping("/sources/{sourceId}/flags/{flagId}")
    @Log
    public ResponseEntity<FlagDTO> addOrUpdateFlag(@PathVariable UUID sourceId,
                                                 @PathVariable String flagId,
                                                 @Valid @RequestBody FlagConfigRequestDTO request) {
        flagService.addOrUpdateFlag(new SourceId(sourceId), flagId, request);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping("/sources/{sourceId}/flags/{flagId}")
    @Log
    public ResponseEntity<Void> deleteFlag(@PathVariable UUID sourceId,
                                       @PathVariable String flagId) {
        flagService.deleteFlag(new SourceId(sourceId), flagId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}