package tech.onova.flagd_admin_server.domain.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class SourceUriTest {

    @Test
    void shouldCreateSourceUriWithValidURI() {
        // Given
        String uri = "file://path/to/source";
        
        // When
        SourceUri sourceUri = new SourceUri(uri);
        
        // Then
        assertThat(sourceUri.uri()).isEqualTo(uri);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "file://valid/path",
        "file:///absolute/path",
        "file://relative/path",
        "file://path/with spaces",
        "file://path/with-dashes",
        "file://path/with_underscores",
        "file://path/with/extension.json",
        "file://path/with/extension.yaml",
        "file://empty"
    })
    void shouldAcceptValidFileUris(String uri) {
        // When
        SourceUri sourceUri = new SourceUri(uri);
        
        // Then
        assertThat(sourceUri.uri()).isEqualTo(uri);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "   ",
        "invalid-uri",
        "http://not-file-protocol",
        "ftp://file-protocol",
        "://missing-protocol",
        "file:",
        "file://",
        "null"
    })
    void shouldRejectInvalidUris(String uri) {
        // When & Then
        if ("null".equals(uri)) {
            assertThatThrownBy(() -> new SourceUri(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Source uri must not be null");
        } else {
            assertThatThrownBy(() -> new SourceUri(uri))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Source uri must start with file://");
        }
    }

    @Test
    void shouldHaveCorrectEquals() {
        // Given
        String uri = "file://test/path";
        SourceUri uri1 = new SourceUri(uri);
        SourceUri uri2 = new SourceUri(uri);
        SourceUri uri3 = new SourceUri("file://different/path");
        
        // Then
        assertThat(uri1).isEqualTo(uri2);
        assertThat(uri1).isNotEqualTo(uri3);
        assertThat(uri1).isNotEqualTo(null);
        assertThat(uri1).isNotEqualTo("string");
    }

    @Test
    void shouldHaveCorrectHashCode() {
        // Given
        String uri = "file://test/path";
        SourceUri uri1 = new SourceUri(uri);
        SourceUri uri2 = new SourceUri(uri);
        
        // Then
        assertThat(uri1.hashCode()).isEqualTo(uri2.hashCode());
    }

    @Test
    void shouldHaveCorrectToString() {
        // Given
        String uri = "file://test/path";
        SourceUri sourceUri = new SourceUri(uri);
        
        // Then
        assertThat(sourceUri.toString()).contains(uri);
    }
}