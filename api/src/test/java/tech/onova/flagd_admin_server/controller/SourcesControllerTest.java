package tech.onova.flagd_admin_server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tech.onova.flagd_admin_server.controller.DTOs.*;
import tech.onova.flagd_admin_server.controller.exception.GlobalExceptionHandler;
import tech.onova.flagd_admin_server.domain.entity.Source;
import tech.onova.flagd_admin_server.domain.entity.SourceId;
import tech.onova.flagd_admin_server.domain.entity.SourceUri;
import tech.onova.flagd_admin_server.domain.exception.SourceContentNotFoundException;
import tech.onova.flagd_admin_server.domain.repository.SourceRepository;
import tech.onova.flagd_admin_server.domain.service.FlagService;
import tech.onova.flagd_admin_server.domain.service.SourceContentService;
import tech.onova.flagd_admin_server.infrastructure.annotation.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
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
        SourcesController sourcesController = new SourcesController(sourceRepository, sourceContentService, flagService);
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

        // When & Then
        mockMvc.perform(get("/api/v1/sources"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void shouldGetSource() throws Exception {
        // Given
        when(sourceRepository.findById(new SourceId(testSourceId))).thenReturn(Optional.of(testSource));

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
    void shouldGetFlags() throws Exception {
        // Given
        List<FlagDTO> flags = List.of(
            new FlagDTO("test-flag", "Test Flag", "A test flag", "ENABLED", "on", Map.of("on", true, "off", false))
        );
        when(flagService.getFlags(new SourceId(testSourceId))).thenReturn(flags);

        // When & Then
        mockMvc.perform(get("/api/v1/sources/{sourceId}/flags", testSourceId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.flags", hasSize(1)))
            .andExpect(jsonPath("$.flags[0].flagId").value("test-flag"))
            .andExpect(jsonPath("$.flags[0].name").value("Test Flag"))
            .andExpect(jsonPath("$.flags[0].state").value("ENABLED"))
            .andExpect(jsonPath("$.flags[0].defaultVariant").value("on"));
    }

    @Test
    void shouldAddOrUpdateFlag() throws Exception {
        // Given
        FlagConfigRequestDTO request = new FlagConfigRequestDTO(
                "Updated Test Flag",
                "Updated Description",
                "ENABLED",
                "on",
                Map.of("on", true, "off", false)
        );
        
        doNothing().when(flagService).addOrUpdateFlag(eq(new SourceId(testSourceId)), eq("test-flag"), any(FlagConfigRequestDTO.class));

        // When & Then
        mockMvc.perform(post("/api/v1/sources/{sourceId}/flags/{flagId}", testSourceId, "test-flag")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());
    }

    @Test
    void shouldDeleteFlag() throws Exception {
        // Given
        doNothing().when(flagService).deleteFlag(eq(new SourceId(testSourceId)), eq("test-flag"));

        // When & Then
        mockMvc.perform(delete("/api/v1/sources/{sourceId}/flags/{flagId}", testSourceId, "test-flag"))
            .andExpect(status().isNoContent());
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

    @Test
    void shouldReturnNotFoundForNonExistentFlag() throws Exception {
        // Given
        when(flagService.getFlags(new SourceId(testSourceId))).thenThrow(new SourceContentNotFoundException("Source not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/sources/{sourceId}/flags", testSourceId))
            .andExpect(status().isNotFound());
    }
}
