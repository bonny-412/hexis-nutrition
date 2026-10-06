package com.hexisnutrition.backend.pazienti;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VisitaRepository extends JpaRepository<Visita, UUID> {
    List<Visita> findAllByPazienteId(UUID pazienteId);

    List<Visita> findAllByPazienteIdOrderByDataVisitaAsc(UUID pazienteId);

    Page<Visita> findAllByPazienteId(UUID pazienteId, Pageable pageable);

    Optional<Visita> findFirstByPazienteIdAndDataVisitaLessThanOrderByDataVisitaDesc(UUID pazienteId, LocalDate dataVisita);

    List<Visita> findAllByPazienteIdIn(List<UUID> pazienteIds);
}
