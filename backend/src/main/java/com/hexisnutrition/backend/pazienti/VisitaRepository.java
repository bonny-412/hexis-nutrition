package com.hexisnutrition.backend.pazienti;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VisitaRepository extends JpaRepository<Visita, UUID> {
    List<Visita> findAllByPazienteId(UUID pazienteId);

    List<Visita> findAllByPazienteIdOrderByDataVisitaAsc(UUID pazienteId);
}
