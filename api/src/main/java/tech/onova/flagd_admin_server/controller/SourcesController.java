package tech.onova.flagd_admin_server.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.onova.flagd_admin_server.controller.DTOs.*;
import tech.onova.flagd_admin_server.domain.entity.Source;
import tech.onova.flagd_admin_server.domain.entity.SourceId;
import tech.onova.flagd_admin_server.domain.entity.SourceUri;
import tech.onova.flagd_admin_server.domain.repository.SourceRepository;
import tech.onova.flagd_admin_server.domain.service.SourceContentService;
import tech.onova.flagd_admin_server.domain.service.FlagService;
import tech.onova.flagd_admin_server.infrastructure.annotation.Log;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class SourcesController {
    private final SourceRepository sourceRepository;
    private final SourceContentService sourceContentService;
    private final FlagService flagService;

    @Autowired
    public SourcesController(SourceRepository sourceRepository, SourceContentService sourceContentService, FlagService flagService){
        this.sourceRepository = sourceRepository;
        this.sourceContentService = sourceContentService;
        this.flagService = flagService;
    }

    @GetMapping("/sources")
    @Log
    public List<SourceResponseDTO> getSources(@RequestParam(defaultValue = "true") boolean isEnabled){
        return sourceRepository.findByEnabled(isEnabled).stream()
                .map(source -> new SourceResponseDTO(
                        source.getId().id(),
                        source.getName(),
                        source.getDescription(),
                        source.getUri().uri(),
                        source.isEnabled(),
                        source.getCreationDateTime(),
                        source.getLastUpdateDateTime(),
                        source.getLastUpdateUserName()
                ))
                .toList();
    }

    @GetMapping("/sources/{sourceId}")
    @Log
    public ResponseEntity<SourceResponseDTO> getSource(@PathVariable UUID sourceId){
        var sourceOption = sourceRepository.findById(new SourceId(sourceId));

        if(sourceOption.isEmpty())
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        var source = sourceOption.get();

        return new ResponseEntity<>(
                new SourceResponseDTO(
                        source.getId().id(),
                        source.getName(),
                        source.getDescription(),
                        source.getUri().uri(),
                        source.isEnabled(),
                        source.getCreationDateTime(),
                        source.getLastUpdateDateTime(),
                        source.getLastUpdateUserName()
                ),
                HttpStatus.OK
        );
    }

    @PatchMapping("/sources/{sourceId}")
    @Log
    public ResponseEntity<SourceResponseDTO> patchSource(@PathVariable UUID sourceId,
                                                         @Valid @RequestBody SourcePatchRequestDTO request){
        var sourcePatch = new Source(
                request.name(),
                request.description(),
                new SourceUri(request.uri()),
                "system",
                request.enabled()
        );

        var sourceOption = sourceRepository.findById(new SourceId(sourceId));

        if(sourceOption.isEmpty())
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        var source = sourceOption.get().update(sourcePatch);
        sourceRepository.save(source);

        return new ResponseEntity<>(
                new SourceResponseDTO(
                        source.getId().id(),
                        source.getName(),
                        source.getDescription(),
                        source.getUri().uri(),
                        source.isEnabled(),
                        source.getCreationDateTime(),
                        source.getLastUpdateDateTime(),
                        source.getLastUpdateUserName()
                ),
                HttpStatus.OK
        );
    }

    @PostMapping("/sources")
    @Log
    public ResponseEntity<SourceResponseDTO> addSource(@Valid @RequestBody SourcePostRequestDTO request){
        var source = new Source(
                request.name(),
                request.description(),
                new SourceUri(request.uri()),
                "system",
                true
        );
        sourceContentService.initializeContentWithConfig(source.getUri());
        sourceRepository.save(source);

        return new ResponseEntity<>(
                 new SourceResponseDTO(
                          source.getId().id(),
                          source.getName(),
                          source.getDescription(),
                          source.getUri().uri(),
                          source.isEnabled(),
                          source.getCreationDateTime(),
                          source.getLastUpdateDateTime(),
                          source.getLastUpdateUserName()),
                  HttpStatus.CREATED);
    }

    @GetMapping("/sources/{sourceId}/contents")
    @Log
    public ResponseEntity<SourceContentResponseDTO> getSourceContents(@PathVariable UUID sourceId) {
        var sourceOption = sourceRepository.findById(new SourceId(sourceId));

        if (sourceOption.isEmpty())
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        var source = sourceOption.get();

        String content = sourceContentService.loadContent(source.getUri());
        return new ResponseEntity<>(
                new SourceContentResponseDTO(content),
                HttpStatus.OK
        );
    }

    @GetMapping("/sources/{sourceId}/flags")
    @Log
    public ResponseEntity<FlagsResponseDTO> getFlags(@PathVariable UUID sourceId){
        List<FlagDTO> flags = flagService.getFlags(new SourceId(sourceId));
        return new ResponseEntity<>(new FlagsResponseDTO(flags), HttpStatus.OK);
    }

    @GetMapping("/sources/{sourceId}/flags/{flagId}")
    @Log
    public ResponseEntity<FlagDTO> getFlag(@PathVariable UUID sourceId, @PathVariable String flagId){
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
                                             @Valid @RequestBody FlagConfigRequestDTO request){
        flagService.addOrUpdateFlag(new SourceId(sourceId), flagId, request);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping("/sources/{sourceId}/flags/{flagId}")
    @Log
    public ResponseEntity<Void> deleteFlag(@PathVariable UUID sourceId,
                                      @PathVariable String flagId){
        flagService.deleteFlag(new SourceId(sourceId), flagId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
