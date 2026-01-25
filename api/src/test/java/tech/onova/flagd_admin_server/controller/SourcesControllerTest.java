package tech.onova.flagd_admin_server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tech.onova.flagd_admin_server.controller.dto.response.*;
import tech.onova.flagd_admin_server.controller.exception.GlobalExceptionHandler;
import tech.onova.flagd_admin_server.controller.mapper.SourceMapper;
import tech.onova.flagd_admin_server.domain.entity.Source;
import tech.onova.flagd_admin_server.domain.entity.SourceId;
import tech.onova.flagd_admin_server.domain.entity.SourceUri;
import tech.onova.flagd_admin_server.domain.repository.SourceRepository;
import tech.onova.flagd_admin_server.domain.service.FlagService;
import tech.onova.flagd_admin_server.domain.service.SourceContentService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;

@ExtendWith(MockitoExtension.class)
class SourcesControllerTest {
    
    private MockMvc mockMvc;
    
    @Mock
    private SourceRepository sourceRepository;
    
    @Mock
    private FlagService flagService;
    
    @Mock
    private SourceContentService sourceContentService;
    
    @Mock
    private SourceMapper sourceMapper;
    
    private ObjectMapper objectMapper = new ObjectMapper();

    private Source testSource = new Source(
            "Test Source",
            "Test Description",
            new SourceUri("file://test-source"),
            "system",
            true
    );

    private final UUID testSourceId = UUID.fromString("00000000-0000-0000-0000-000000001");

    @BeforeEach
    void setUp() {
        SourcesController sourcesController = new SourcesController(sourceRepository, sourceContentService, sourceMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(sourcesController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldGetSources() throws Exception {
        // Given
        Source enabledSource = new Source(
                "Enabled Source",
                "Enabled Description",
                new SourceUri("file://enabled-source"),
                "system",
                true
        );
        
        when(sourceRepository.findByEnabled(true)).thenReturn(List.of(enabledSource));
        when(sourceMapper.toResponseDTO(enabledSource)).thenReturn(
                new SourceResponseDTO(
                        enabledSource.getId().id(),
                        enabledSource.getName(),
                        enabledSource.getDescription(),
                        enabledSource.getUri().uri(),
                        enabledSource.isEnabled(),
                        enabledSource.getCreationDateTime(),
                        enabledSource.getLastUpdateDateTime(),
                        enabledSource.getLastUpdateUserName()
                )
        );

        // When & Then
        mockMvc.perform(get("/api/v1/sources"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void shouldGetSource() throws Exception {
        // Given
        when(sourceRepository.findById(new SourceId(testSourceId))).thenReturn(Optional.of(testSource));
        when(sourceMapper.toResponseDTO(testSource)).thenReturn(
                new SourceResponseDTO(
                        testSource.getId().id(),
                        testSource.getName(),
                        testSource.getDescription(),
                        testSource.getUri().uri(),
                        testSource.isEnabled(),
                        testSource.getCreationDateTime(),
                        testSource.getLastUpdateDateTime(),
                        testSource.getLastUpdateUserName()
                )
        );

        // When & Then
        mockMvc.perform(get("/api/v1/sources/{sourceId}", testSourceId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Test Source"))
            .andExpect(jsonPath("$.description").value("Test Description"))
            .andExpect(jsonPath("$.uri").value("file://test-source"))
            .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void shouldGetSourceContents() throws Exception {
        // Given
        when(sourceRepository.findById(new SourceId(testSourceId))).thenReturn(Optional.of(testSource));
        when(sourceContentService.loadContent(testSource.getUri())).thenReturn("file content");

        // When & Then
        mockMvc.perform(get("/api/v1/sources/{sourceId}/contents", testSourceId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value("file content"));
    }

    @Test
    void shouldReturnNotFoundForNonExistentSource() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(sourceRepository.findById(new SourceId(nonExistentId))).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/v1/sources/{sourceId}", nonExistentId))
            .andExpect(status().isNotFound());
    }
}
