package com.hexisnutrition.backend.pianialimentari;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PianoAlimentareRepository extends JpaRepository<PianoAlimentare, UUID>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<PianoAlimentare> {
    List<PianoAlimentare> findAllByPazienteIdAndStato(UUID pazienteId, StatoPiano stato);
}
