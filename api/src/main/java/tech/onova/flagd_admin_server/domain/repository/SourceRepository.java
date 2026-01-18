package tech.onova.flagd_admin_server.domain.repository;

import org.springframework.data.repository.CrudRepository;
import tech.onova.flagd_admin_server.domain.entity.Source;
import tech.onova.flagd_admin_server.domain.entity.SourceId;

import java.util.List;

public interface SourceRepository extends CrudRepository<Source, SourceId> {
    List<Source> findByEnabled(boolean enabled);
}
