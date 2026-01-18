package tech.onova.flagd_admin_server.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.onova.flagd_admin_server.domain.entity.SourceUri;
import tech.onova.flagd_admin_server.domain.exception.ContentValidationException;
import tech.onova.flagd_admin_server.domain.exception.SourceContentAccessException;
import tech.onova.flagd_admin_server.domain.exception.SourceContentNotFoundException;
import tech.onova.flagd_admin_server.domain.exception.UnsupportedSourceUriException;
import tech.onova.flagd_admin_server.domain.service.impl.SourceContentServiceImpl;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SourceContentServiceImplTest {

    @Mock
    private SourceContentLoader fileLoader;
    
    @Mock
    private SourceContentLoader httpLoader;
    
    @Mock
    private ContentValidator contentValidator;

    private SourceContentServiceImpl service;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        List<SourceContentLoader> loaders = List.of(fileLoader, httpLoader);
        service = new SourceContentServiceImpl(loaders, contentValidator);
    }

    @Test
    void shouldLoadContentWithFileUri() throws Exception {
        // Given
        SourceUri sourceUri = new SourceUri("file://test/path");
        String expectedContent = "test content";
        
        when(fileLoader.supports("file")).thenReturn(true);
        when(fileLoader.loadContent(sourceUri)).thenReturn(expectedContent);

        // When
        String actualContent = service.loadContent(sourceUri);

        // Then
        assertThat(actualContent).isEqualTo(expectedContent);
        verify(fileLoader).loadContent(sourceUri);
        verify(httpLoader, never()).loadContent(any());
    }

    @Test
    void shouldPropagateDomainExceptionsForLoadContent() throws Exception {
        // Given
        SourceUri sourceUri = new SourceUri("file://test/path");
        SourceContentNotFoundException notFoundException = new SourceContentNotFoundException("File not found");
        
        when(fileLoader.supports("file")).thenReturn(true);
        when(fileLoader.loadContent(sourceUri)).thenThrow(notFoundException);

        // When & Then
        assertThatThrownBy(() -> service.loadContent(sourceUri))
            .isInstanceOf(SourceContentNotFoundException.class)
            .isSameAs(notFoundException);
    }

    @Test
    void shouldInitializeContentAfterValidation() throws Exception {
        // Given
        SourceUri sourceUri = new SourceUri("file://test/path");
        String content = "test content";
        
        when(fileLoader.supports("file")).thenReturn(true);
        doNothing().when(contentValidator).validateContent(content);
        doNothing().when(fileLoader).initializeContent(sourceUri, content);

        // When
        service.initializeContent(sourceUri, content);

        // Then
        verify(contentValidator).validateContent(content);
        verify(fileLoader).initializeContent(sourceUri, content);
        verify(httpLoader, never()).initializeContent(any(), any());
    }

    @Test
    void shouldNotInitializeContentWhenValidationFails() throws Exception {
        // Given
        SourceUri sourceUri = new SourceUri("file://test/path");
        String content = "invalid content";
        ContentValidationException validationException = new ContentValidationException("Invalid content");
        
        doThrow(validationException).when(contentValidator).validateContent(content);

        // When & Then
        assertThatThrownBy(() -> service.initializeContent(sourceUri, content))
            .isInstanceOf(ContentValidationException.class)
            .isSameAs(validationException);
        
        verify(fileLoader, never()).initializeContent(any(), any());
        verify(httpLoader, never()).initializeContent(any(), any());
    }

    @Test
    void shouldPropagateSourceContentAccessExceptionForInitializeContent() throws Exception {
        // Given
        SourceUri sourceUri = new SourceUri("file://test/path");
        String content = "test content";
        SourceContentAccessException accessException = new SourceContentAccessException("Access denied");
        
        when(fileLoader.supports("file")).thenReturn(true);
        doNothing().when(contentValidator).validateContent(content);
        doThrow(accessException).when(fileLoader).initializeContent(sourceUri, content);

        // When & Then
        assertThatThrownBy(() -> service.initializeContent(sourceUri, content))
            .isInstanceOf(SourceContentAccessException.class)
            .isSameAs(accessException);
    }

    @Test
    void shouldThrowUnsupportedUriExceptionWhenNoLoaderSupportsScheme() throws Exception {
        // Given
        SourceUri sourceUri = new SourceUri("file://test/path");
        String content = "test content";
        
        when(fileLoader.supports("file")).thenReturn(false);
        when(httpLoader.supports("file")).thenReturn(false);
        doNothing().when(contentValidator).validateContent(content);

        // When & Then
        assertThatThrownBy(() -> service.initializeContent(sourceUri, content))
            .isInstanceOf(UnsupportedSourceUriException.class)
            .hasMessageContaining("Unsupported URI scheme: file");
    }

    @Test
    void shouldReturnTrueWhenContentExists() throws Exception {
        // Given
        SourceUri sourceUri = new SourceUri("file://test/path");
        
        when(fileLoader.supports("file")).thenReturn(true);
        when(fileLoader.loadContent(sourceUri)).thenReturn("existing content");

        // When
        boolean result = service.contentExists(sourceUri);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenContentDoesNotExist() throws Exception {
        // Given
        SourceUri sourceUri = new SourceUri("file://test/path");
        
        when(fileLoader.supports("file")).thenReturn(true);
        when(fileLoader.loadContent(sourceUri)).thenThrow(new SourceContentNotFoundException("File not found"));

        // When
        boolean result = service.contentExists(sourceUri);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalseWhenNoLoaderSupportsScheme() throws Exception {
        // Given
        SourceUri sourceUri = new SourceUri("file://test/path");
        
        when(fileLoader.supports("file")).thenReturn(false);
        when(httpLoader.supports("file")).thenReturn(false);

        // When
        boolean result = service.contentExists(sourceUri);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldInitializeContentWithConfigAfterValidation() throws Exception {
        // Given
        SourceUri sourceUri = new SourceUri("file://test/path");
        
        when(fileLoader.supports("file")).thenReturn(true);
        doNothing().when(fileLoader).initializeContent(sourceUri, null);

        // When
        service.initializeContentWithConfig(sourceUri);

        // Then
        verify(fileLoader).initializeContent(sourceUri, null);
        verify(httpLoader, never()).initializeContent(any(), any());
    }

    @Test
    void shouldPropagateSourceContentAccessExceptionForInitializeContentWithConfig() throws Exception {
        // Given
        SourceUri sourceUri = new SourceUri("file://test/path");
        SourceContentAccessException accessException = new SourceContentAccessException("Access denied");
        
        when(fileLoader.supports("file")).thenReturn(true);
        doThrow(accessException).when(fileLoader).initializeContent(sourceUri, null);

        // When & Then
        assertThatThrownBy(() -> service.initializeContentWithConfig(sourceUri))
            .isInstanceOf(SourceContentAccessException.class)
            .isSameAs(accessException);
    }
}