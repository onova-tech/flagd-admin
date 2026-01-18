package tech.onova.flagd_admin_server.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.onova.flagd_admin_server.domain.entity.SourceUri;
import tech.onova.flagd_admin_server.domain.exception.SourceContentAccessException;
import tech.onova.flagd_admin_server.domain.service.impl.FileSourceContentLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class FileSourceContentLoaderTest {

    private FileSourceContentLoader loader;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        loader = new FileSourceContentLoader();
    }

    @Test
    void shouldSupportFileUriScheme() {
        // When & Then
        assertThat(loader.supports("file")).isTrue();
        assertThat(loader.supports("FILE")).isTrue();
        assertThat(loader.supports("http")).isFalse();
        assertThat(loader.supports("https")).isFalse();
        assertThat(loader.supports("ftp")).isFalse();
    }

    @Test
    void shouldLoadContentFromExistingFile() throws Exception {
        // Given
        String content = "test file content";
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, content);
        
        SourceUri sourceUri = new SourceUri("file://" + testFile.toString());

        // When
        String loadedContent = loader.loadContent(sourceUri);

        // Then
        assertThat(loadedContent).isEqualTo(content);
    }

    @Test
    void shouldLoadContentFromExistingFileWithSpecialCharacters() throws Exception {
        // Given
        String content = "test content with special chars: ñáéíóú";
        Path testFile = tempDir.resolve("test-special.txt");
        Files.writeString(testFile, content);
        
        SourceUri sourceUri = new SourceUri("file://" + testFile.toString());

        // When
        String loadedContent = loader.loadContent(sourceUri);

        // Then
        assertThat(loadedContent).isEqualTo(content);
    }

    @Test
    void shouldThrowExceptionForNonExistentFile() {
        // Given
        SourceUri sourceUri = new SourceUri("file://non/existent/path.txt");

        // When & Then
        assertThatThrownBy(() -> loader.loadContent(sourceUri))
            .isInstanceOf(Exception.class)
            .hasMessageContaining("File not found");
    }



    @Test
    void shouldInitializeContentInNewFile() throws Exception {
        // Given
        String content = "initial content";
        Path testFile = tempDir.resolve("new-file.txt");
        SourceUri sourceUri = new SourceUri("file://" + testFile.toString());

        // When
        loader.initializeContent(sourceUri, content);

        // Then
        assertThat(Files.exists(testFile)).isTrue();
        assertThat(Files.readString(testFile)).isEqualTo(content);
    }

    @Test
    void shouldInitializeContentInNewDirectory() throws Exception {
        // Given
        String content = "content in nested dir";
        Path nestedDir = tempDir.resolve("nested").resolve("directory");
        Path testFile = nestedDir.resolve("file.txt");
        SourceUri sourceUri = new SourceUri("file://" + testFile.toString());

        // When
        loader.initializeContent(sourceUri, content);

        // Then
        assertThat(Files.exists(testFile)).isTrue();
        assertThat(Files.readString(testFile)).isEqualTo(content);
    }

    @Test
    void shouldOverwriteContentInExistingFile() throws Exception {
        // Given
        String originalContent = "original content";
        String newContent = "new content";
        Path testFile = tempDir.resolve("existing.txt");
        Files.writeString(testFile, originalContent);
        
        SourceUri sourceUri = new SourceUri("file://" + testFile.toString());

        // When
        loader.initializeContent(sourceUri, newContent);

        // Then
        assertThat(Files.readString(testFile)).isEqualTo(newContent);
    }

    @Test
    void shouldThrowExceptionWhenInitializeContentFails() {
        // Given
        String content = "content";
        // Try to write to a directory instead of a file
        SourceUri sourceUri = new SourceUri("file://" + tempDir.toString());
        
        // When & Then
        assertThatThrownBy(() -> loader.initializeContent(sourceUri, content))
            .isInstanceOf(SourceContentAccessException.class);
    }

    @Test
    void shouldHandleEmptyContent() throws Exception {
        // Given
        String content = "";
        Path testFile = tempDir.resolve("empty.txt");
        SourceUri sourceUri = new SourceUri("file://" + testFile.toString());

        // When
        loader.initializeContent(sourceUri, content);

        // Then
        assertThat(Files.exists(testFile)).isTrue();
        assertThat(Files.readString(testFile)).isEmpty();
    }

    @Test
    void shouldHandleLargeContent() throws Exception {
        // Given
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeContent.append("This is line ").append(i).append(" of the large content.\n");
        }
        String content = largeContent.toString();
        
        Path testFile = tempDir.resolve("large.txt");
        SourceUri sourceUri = new SourceUri("file://" + testFile.toString());

        // When
        loader.initializeContent(sourceUri, content);

        // Then
        assertThat(Files.exists(testFile)).isTrue();
        assertThat(Files.readString(testFile)).isEqualTo(content);
    }
}