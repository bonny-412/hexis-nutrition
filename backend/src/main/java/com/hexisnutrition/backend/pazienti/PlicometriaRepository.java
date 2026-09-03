package com.hexisnutrition.backend.pazienti;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlicometriaRepository extends JpaRepository<Plicometria, UUID> {
    Optional<Plicometria> findByVisitaId(UUID visitaId);

    void deleteByVisitaId(UUID visitaId);
}
