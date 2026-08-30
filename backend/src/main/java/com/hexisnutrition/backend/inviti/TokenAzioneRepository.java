package com.hexisnutrition.backend.inviti;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TokenAzioneRepository extends JpaRepository<TokenAzione, UUID> {
    Optional<TokenAzione> findByTokenHash(String tokenHash);

    void deleteByTipoAndProfessionistaId(TipoToken tipo, UUID professionistaId);

    void deleteByTipoAndPazienteId(TipoToken tipo, UUID pazienteId);

    void deleteByScadenzaBefore(Instant istante);
}
