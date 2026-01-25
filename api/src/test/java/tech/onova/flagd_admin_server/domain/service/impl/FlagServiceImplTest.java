package tech.onova.flagd_admin_server.domain.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.onova.flagd_admin_server.controller.dto.request.FlagConfigRequestDTO;
import tech.onova.flagd_admin_server.controller.dto.response.FlagDTO;
import tech.onova.flagd_admin_server.domain.entity.Source;
import tech.onova.flagd_admin_server.domain.entity.SourceId;
import tech.onova.flagd_admin_server.domain.entity.SourceUri;
import tech.onova.flagd_admin_server.domain.exception.SourceContentNotFoundException;
import tech.onova.flagd_admin_server.domain.repository.SourceRepository;
import tech.onova.flagd_admin_server.domain.service.SourceContentService;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlagServiceImplTest {

    @Mock
    private SourceRepository sourceRepository;

    @Mock
    private SourceContentService sourceContentService;

    private FlagServiceImpl flagService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String validFlagdContent = """
        {
          "$schema": "https://flagd.dev/schema/v0/flags.json",
          "flags": {
            "my-flag": {
              "key": "my-flag",
              "name": "My Flag",
              "description": "A test flag",
              "state": "ENABLED",
              "defaultVariant": "on",
              "variants": {
                "on": true,
                "off": false
              },
              "targeting": {
                "targetingKey": {
                  "userId": "string"
                },
                "rule": "if (userId in ['user1', 'user2']) return true"
              }
            },
            "another-flag": {
              "key": "another-flag",
              "name": "Another Flag",
              "state": "DISABLED",
              "defaultVariant": "off"
            }
          }
        }
        """;

    @BeforeEach
    void setUp() {
        flagService = new FlagServiceImpl(sourceRepository, sourceContentService);
    }

    @Test
    void getFlags_ShouldReturnListOfFlags_WhenSourceExists() {
        // Given
        SourceId sourceId = new SourceId(UUID.randomUUID());
        Source source = new Source("Test Source", "Description", new SourceUri("file://test/path"), "user", true);
        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(sourceContentService.loadContent(any())).thenReturn(validFlagdContent);

        // When
        var flags = flagService.getFlags(sourceId);

        // Then
        assertThat(flags).hasSize(2);
        assertThat(flags.get(0).flagId()).isEqualTo("my-flag");
        assertThat(flags.get(0).name()).isEqualTo("My Flag");
        assertThat(flags.get(0).state()).isEqualTo("ENABLED");
        assertThat(flags.get(0).defaultVariant()).isEqualTo("on");
        assertThat(flags.get(0).variants()).isNotNull();
        assertThat(flags.get(0).variants()).containsEntry("on", true);
        assertThat(flags.get(0).variants()).containsEntry("off", false);
        assertThat(flags.get(0).targeting()).isNotNull();
        
        assertThat(flags.get(1).flagId()).isEqualTo("another-flag");
        assertThat(flags.get(1).name()).isEqualTo("Another Flag");
        assertThat(flags.get(1).state()).isEqualTo("DISABLED");
    }

    @Test
    void getFlags_ShouldThrowException_WhenSourceNotFound() {
        // Given
        SourceId sourceId = new SourceId(UUID.randomUUID());
        when(sourceRepository.findById(sourceId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> flagService.getFlags(sourceId))
            .isInstanceOf(SourceContentNotFoundException.class)
            .hasMessageContaining("Source not found");
    }

    @Test
    void getFlags_ShouldReturnEmptyList_WhenNoFlags() {
        // Given
        SourceId sourceId = new SourceId(UUID.randomUUID());
        Source source = new Source("Test Source", "Description", new SourceUri("file://test/path"), "user", true);
        String emptyContent = "{\"flags\": {}}";
        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(sourceContentService.loadContent(any())).thenReturn(emptyContent);

        // When
        var flags = flagService.getFlags(sourceId);

        // Then
        assertThat(flags).isEmpty();
    }

    @Test
    void getFlags_ShouldHandleNullFields() {
        // Given
        SourceId sourceId = new SourceId(UUID.randomUUID());
        Source source = new Source("Test Source", "Description", new SourceUri("file://test/path"), "user", true);
        String content = """
            {
              "$schema": "https://flagd.dev/schema/v0/flags.json",
              "flags": {
                "minimal-flag": {
                  "key": "minimal-flag"
                }
              }
            }
            """;
        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(sourceContentService.loadContent(any())).thenReturn(content);

        // When
        var flags = flagService.getFlags(sourceId);

        // Then
        assertThat(flags).hasSize(1);
        FlagDTO flag = flags.get(0);
        assertThat(flag.flagId()).isEqualTo("minimal-flag");
        assertThat(flag.name()).isNull();
        assertThat(flag.description()).isNull();
        assertThat(flag.state()).isNull();
        assertThat(flag.defaultVariant()).isNull();
        assertThat(flag.variants()).isNull();
        assertThat(flag.targeting()).isNull();
    }

    @Test
    void getFlag_ShouldReturnFlag_WhenFlagExists() {
        // Given
        SourceId sourceId = new SourceId(UUID.randomUUID());
        Source source = new Source("Test Source", "Description", new SourceUri("file://test/path"), "user", true);
        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(sourceContentService.loadContent(any())).thenReturn(validFlagdContent);

        // When
        FlagDTO flag = flagService.getFlag(sourceId, "my-flag");

        // Then
        assertThat(flag).isNotNull();
        assertThat(flag.flagId()).isEqualTo("my-flag");
        assertThat(flag.name()).isEqualTo("My Flag");
        assertThat(flag.description()).isEqualTo("A test flag");
        assertThat(flag.state()).isEqualTo("ENABLED");
        assertThat(flag.defaultVariant()).isEqualTo("on");
    }

    @Test
    void getFlag_ShouldReturnNull_WhenFlagNotFound() {
        // Given
        SourceId sourceId = new SourceId(UUID.randomUUID());
        Source source = new Source("Test Source", "Description", new SourceUri("file://test/path"), "user", true);
        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(sourceContentService.loadContent(any())).thenReturn(validFlagdContent);

        // When
        FlagDTO flag = flagService.getFlag(sourceId, "non-existent-flag");

        // Then
        assertThat(flag).isNull();
    }

    @Test
    void getFlag_ShouldThrowException_WhenSourceNotFound() {
        // Given
        SourceId sourceId = new SourceId(UUID.randomUUID());
        when(sourceRepository.findById(sourceId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> flagService.getFlag(sourceId, "my-flag"))
            .isInstanceOf(SourceContentNotFoundException.class)
            .hasMessageContaining("Source not found");
    }

    @Test
    void addOrUpdateFlag_ShouldAddFlag_WhenFlagDoesNotExist() {
        // Given
        SourceId sourceId = new SourceId(UUID.randomUUID());
        Source source = new Source("Test Source", "Description", new SourceUri("file://test/path"), "user", true);
        String content = "{\"flags\": {}}";
        FlagConfigRequestDTO request = new FlagConfigRequestDTO(
            "New Flag",
            "A new test flag",
            "ENABLED",
            "on",
            Map.of("on", true, "off", false),
            Map.of("targetingKey", Map.of("userId", "string"), "rule", "if (userId == 'test') return true")
        );
        
        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(sourceContentService.loadContent(any())).thenReturn(content);
        
        // When
        flagService.addOrUpdateFlag(sourceId, "new-flag", request);
        
        // Then
        verify(sourceContentService).initializeContent(any(), anyString());
    }

    @Test
    void addOrUpdateFlag_ShouldUpdateFlag_WhenFlagExists() {
        // Given
        SourceId sourceId = new SourceId(UUID.randomUUID());
        Source source = new Source("Test Source", "Description", new SourceUri("file://test/path"), "user", true);
        FlagConfigRequestDTO request = new FlagConfigRequestDTO(
            "Updated Flag",
            "Updated description",
            "DISABLED",
            "off",
            Map.of("on", true, "off", false),
            null
        );
        
        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(sourceContentService.loadContent(any())).thenReturn(validFlagdContent);
        
        // When
        flagService.addOrUpdateFlag(sourceId, "my-flag", request);
        
        // Then
        verify(sourceContentService).initializeContent(any(), anyString());
    }

    @Test
    void addOrUpdateFlag_ShouldThrowException_WhenSourceNotFound() {
        // Given
        SourceId sourceId = new SourceId(UUID.randomUUID());
        FlagConfigRequestDTO request = new FlagConfigRequestDTO(
            "New Flag",
            "Description",
            "ENABLED",
            "on",
            Map.of("on", true),
            null
        );
        
        when(sourceRepository.findById(sourceId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> flagService.addOrUpdateFlag(sourceId, "new-flag", request))
            .isInstanceOf(SourceContentNotFoundException.class)
            .hasMessageContaining("Source not found");
    }

    @Test
    void addOrUpdateFlag_ShouldHandleEmptyContent() {
        // Given
        SourceId sourceId = new SourceId(UUID.randomUUID());
        Source source = new Source("Test Source", "Description", new SourceUri("file://test/path"), "user", true);
        String emptyContent = "{}";
        FlagConfigRequestDTO request = new FlagConfigRequestDTO(
            "New Flag",
            "Description",
            "ENABLED",
            "on",
            Map.of("on", true),
            null
        );
        
        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(sourceContentService.loadContent(any())).thenReturn(emptyContent);
        
        // When
        flagService.addOrUpdateFlag(sourceId, "new-flag", request);
        
        // Then
        verify(sourceContentService).initializeContent(any(), anyString());
    }

    @Test
    void deleteFlag_ShouldDeleteFlag_WhenFlagExists() {
        // Given
        SourceId sourceId = new SourceId(UUID.randomUUID());
        Source source = new Source("Test Source", "Description", new SourceUri("file://test/path"), "user", true);
        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(sourceContentService.loadContent(any())).thenReturn(validFlagdContent);
        
        // When
        flagService.deleteFlag(sourceId, "my-flag");
        
        // Then
        verify(sourceContentService).initializeContent(any(), anyString());
    }

    @Test
    void deleteFlag_ShouldDoNothing_WhenFlagDoesNotExist() {
        // Given
        SourceId sourceId = new SourceId(UUID.randomUUID());
        Source source = new Source("Test Source", "Description", new SourceUri("file://test/path"), "user", true);
        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(sourceContentService.loadContent(any())).thenReturn(validFlagdContent);
        
        // When
        flagService.deleteFlag(sourceId, "non-existent-flag");
        
        // Then - initializeContent should not be called when flag doesn't exist
        verify(sourceContentService, never()).initializeContent(any(), anyString());
    }

    @Test
    void deleteFlag_ShouldThrowException_WhenSourceNotFound() {
        // Given
        SourceId sourceId = new SourceId(UUID.randomUUID());
        when(sourceRepository.findById(sourceId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> flagService.deleteFlag(sourceId, "my-flag"))
            .isInstanceOf(SourceContentNotFoundException.class)
            .hasMessageContaining("Source not found");
    }

    @Test
    void addOrUpdateFlag_ShouldHandleMinimalRequest() {
        // Given
        SourceId sourceId = new SourceId(UUID.randomUUID());
        Source source = new Source("Test Source", "Description", new SourceUri("file://test/path"), "user", true);
        String content = "{\"flags\": {}}";
        FlagConfigRequestDTO request = new FlagConfigRequestDTO(
            "Minimal Flag",
            null,
            "ENABLED",
            null,
            null,
            null
        );
        
        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(sourceContentService.loadContent(any())).thenReturn(content);
        
        // When
        flagService.addOrUpdateFlag(sourceId, "minimal-flag", request);
        
        // Then
        verify(sourceContentService).initializeContent(any(), anyString());
    }

    @Test
    void getFlag_ShouldReturnNull_WhenFlagKeyIsMissing() {
        // Given
        SourceId sourceId = new SourceId(UUID.randomUUID());
        Source source = new Source("Test Source", "Description", new SourceUri("file://test/path"), "user", true);
        String content = """
            {
              "flags": {
                "invalid-flag": {
                  "name": "Invalid Flag"
                }
              }
            }
            """;
        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(sourceContentService.loadContent(any())).thenReturn(content);

        // When
        FlagDTO flag = flagService.getFlag(sourceId, "invalid-flag");

        // Then
        assertThat(flag).isNull();
    }
}
