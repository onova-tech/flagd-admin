package tech.onova.flagd_admin_server.controller.flags;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tech.onova.flagd_admin_server.controller.FlagsController;
import tech.onova.flagd_admin_server.controller.dto.request.FlagConfigRequestDTO;
import tech.onova.flagd_admin_server.controller.dto.response.FlagDTO;
import tech.onova.flagd_admin_server.controller.dto.response.FlagsResponseDTO;
import tech.onova.flagd_admin_server.controller.exception.GlobalExceptionHandler;
import tech.onova.flagd_admin_server.domain.service.FlagService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;

@ExtendWith(MockitoExtension.class)
class FlagsControllerTest {

    private MockMvc mockMvc;
    
    @Mock
    private FlagService flagService;
    
    private ObjectMapper objectMapper = new ObjectMapper();

    private final UUID testSourceId = UUID.fromString("00000000-0000-0000-0000-000000001");

    @BeforeEach
    void setUp() {
        FlagsController flagsController = new FlagsController(flagService);
        mockMvc = MockMvcBuilders.standaloneSetup(flagsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldGetFlags() throws Exception {
        // Given
        List<FlagDTO> flags = List.of(
                new FlagDTO("test-flag", "Test Flag", "A test flag", "ENABLED", "on", Map.of("on", true, "off", false), null)
        );
        FlagsResponseDTO expectedResponse = new FlagsResponseDTO(flags);
        when(flagService.getFlags(new tech.onova.flagd_admin_server.domain.entity.SourceId(testSourceId))).thenReturn(flags);

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
    void shouldGetFlag() throws Exception {
        // Given
        FlagDTO flag = new FlagDTO("test-flag", "Test Flag", "A test flag", "ENABLED", "on", Map.of("on", true, "off", false), null);
        when(flagService.getFlag(new tech.onova.flagd_admin_server.domain.entity.SourceId(testSourceId), "test-flag")).thenReturn(flag);

        // When & Then
        mockMvc.perform(get("/api/v1/sources/{sourceId}/flags/{flagId}", testSourceId, "test-flag"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.flagId").value("test-flag"))
            .andExpect(jsonPath("$.name").value("Test Flag"))
            .andExpect(jsonPath("$.description").value("A test flag"))
            .andExpect(jsonPath("$.state").value("ENABLED"))
            .andExpect(jsonPath("$.defaultVariant").value("on"));
    }

    @Test
    void shouldReturnNotFoundForNonExistentFlag() throws Exception {
        // Given
        when(flagService.getFlag(new tech.onova.flagd_admin_server.domain.entity.SourceId(testSourceId), "non-existent-flag")).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/v1/sources/{sourceId}/flags/{flagId}", testSourceId, "non-existent-flag"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldAddOrUpdateFlag() throws Exception {
        // Given
        FlagConfigRequestDTO request = new FlagConfigRequestDTO(
                "Updated Test Flag",
                "Updated Description",
                "ENABLED",
                "on",
                Map.of("on", true, "off", false),
                null
        );
        
        doNothing().when(flagService).addOrUpdateFlag(eq(new tech.onova.flagd_admin_server.domain.entity.SourceId(testSourceId)), eq("test-flag"), any(FlagConfigRequestDTO.class));

        // When & Then
        mockMvc.perform(post("/api/v1/sources/{sourceId}/flags/{flagId}", testSourceId, "test-flag")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldDeleteFlag() throws Exception {
        // Given
        doNothing().when(flagService).deleteFlag(eq(new tech.onova.flagd_admin_server.domain.entity.SourceId(testSourceId)), eq("test-flag"));

        // When & Then
        mockMvc.perform(delete("/api/v1/sources/{sourceId}/flags/{flagId}", testSourceId, "test-flag"))
                .andExpect(status().isNoContent());
    }
}