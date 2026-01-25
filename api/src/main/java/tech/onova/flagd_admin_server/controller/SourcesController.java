package tech.onova.flagd_admin_server.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.onova.flagd_admin_server.controller.dto.request.SourcePatchRequestDTO;
import tech.onova.flagd_admin_server.controller.dto.request.SourcePostRequestDTO;
import tech.onova.flagd_admin_server.controller.dto.response.SourceContentResponseDTO;
import tech.onova.flagd_admin_server.controller.dto.response.SourceResponseDTO;
import tech.onova.flagd_admin_server.controller.mapper.SourceMapper;
import tech.onova.flagd_admin_server.domain.entity.Source;
import tech.onova.flagd_admin_server.domain.entity.SourceId;
import tech.onova.flagd_admin_server.domain.entity.SourceUri;
import tech.onova.flagd_admin_server.domain.repository.SourceRepository;
import tech.onova.flagd_admin_server.domain.service.SourceContentService;
import tech.onova.flagd_admin_server.infrastructure.annotation.Log;
import tech.onova.flagd_admin_server.security.AuthenticationUtil;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class SourcesController {
    private final SourceRepository sourceRepository;
    private final SourceContentService sourceContentService;
    private final SourceMapper sourceMapper;

    @Autowired
    public SourcesController(SourceRepository sourceRepository, 
                         SourceContentService sourceContentService,
                         SourceMapper sourceMapper) {
        this.sourceRepository = sourceRepository;
        this.sourceContentService = sourceContentService;
        this.sourceMapper = sourceMapper;
    }

    @GetMapping("/sources")
    @Log
    public List<SourceResponseDTO> getSources(@RequestParam(defaultValue = "true") boolean isEnabled) {
        return sourceRepository.findByEnabled(isEnabled).stream()
                .map(sourceMapper::toResponseDTO)
                .toList();
    }

    @GetMapping("/sources/{sourceId}")
    @Log
    public ResponseEntity<SourceResponseDTO> getSource(@PathVariable UUID sourceId) {
        var sourceOption = sourceRepository.findById(new SourceId(sourceId));

        return sourceOption.map(source -> new ResponseEntity<>(
                sourceMapper.toResponseDTO(source),
                HttpStatus.OK
        )).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PatchMapping("/sources/{sourceId}")
    @Log
    public ResponseEntity<SourceResponseDTO> patchSource(@PathVariable UUID sourceId,
                                                     @Valid @RequestBody SourcePatchRequestDTO request) {
        var sourcePatch = new Source(
                request.name(),
                request.description(),
                new SourceUri(request.uri()),
                AuthenticationUtil.getCurrentUsername(),
                request.enabled()
        );

        var sourceOption = sourceRepository.findById(new SourceId(sourceId));

        if (sourceOption.isEmpty())
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        var source = sourceOption.get().update(sourcePatch);
        sourceRepository.save(source);

        return new ResponseEntity<>(
                sourceMapper.toResponseDTO(source),
                HttpStatus.OK
        );
    }

    @PostMapping("/sources")
    @Log
    public ResponseEntity<SourceResponseDTO> addSource(@Valid @RequestBody SourcePostRequestDTO request) {
        var source = new Source(
                request.name(),
                request.description(),
                new SourceUri(request.uri()),
                AuthenticationUtil.getCurrentUsername(),
                true
        );
        sourceContentService.initializeContentWithConfig(source.getUri());
        sourceRepository.save(source);

        return new ResponseEntity<>(
                sourceMapper.toResponseDTO(source),
                HttpStatus.CREATED);
    }

    @DeleteMapping("/sources/{sourceId}")
    @Log
    public ResponseEntity<Void> deleteSource(@PathVariable UUID sourceId) {
        var sourceOption = sourceRepository.findById(new SourceId(sourceId));

        if (sourceOption.isEmpty())
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        var source = sourceOption.get();
        sourceRepository.delete(source);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
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
}