package tech.onova.flagd_admin_server.controller.mapper;

import org.junit.jupiter.api.Test;
import tech.onova.flagd_admin_server.controller.dto.response.SourceResponseDTO;
import tech.onova.flagd_admin_server.domain.entity.Source;
import tech.onova.flagd_admin_server.testutil.TestDataBuilder;

import static org.assertj.core.api.Assertions.*;

class SourceMapperTest {

    private final SourceMapper mapper = new SourceMapper();

    @Test
    void shouldMapSourceToResponseDTO() {
        // Given
        Source source = TestDataBuilder.aSource()
                .withName("Test Source")
                .withDescription("Test Description")
                .withUri("file://test-source")
                .withLastUpdateUser("test-user")
                .withEnabled(true)
                .build();

        // When
        SourceResponseDTO result = mapper.toResponseDTO(source);

        // Then
        assertThat(result.id()).isEqualTo(source.getId().id());
        assertThat(result.name()).isEqualTo("Test Source");
        assertThat(result.description()).isEqualTo("Test Description");
        assertThat(result.uri()).isEqualTo("file://test-source");
        assertThat(result.enabled()).isTrue();
        assertThat(result.lastUpdateUserName()).isEqualTo("test-user");
        assertThat(result.creationDateTime()).isNotNull();
        assertThat(result.lastUpdateDateTime()).isNotNull();
    }
}