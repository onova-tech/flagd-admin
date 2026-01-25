package tech.onova.flagd_admin_server.domain.repository;

import org.springframework.data.repository.CrudRepository;
import tech.onova.flagd_admin_server.domain.entity.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
    Optional<RefreshToken> findById(String id);
}