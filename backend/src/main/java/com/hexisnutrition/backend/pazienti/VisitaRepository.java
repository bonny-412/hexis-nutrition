package com.hexisnutrition.backend.pazienti;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VisitaRepository extends JpaRepository<Visita, UUID> {
    List<Visita> findAllByPazienteId(UUID pazienteId);

    /** Ordine cronologico completo: a parità di data vale l'ordine di inserimento (collaudo, bug 03 e 12). */
    List<Visita> findAllByPazienteIdOrderByDataVisitaAscCreatoIlAsc(UUID pazienteId);

    Page<Visita> findAllByPazienteId(UUID pazienteId, Pageable pageable);

    /** Visite che vengono prima di quella indicata nell'ordine (data, creazione), dalla più vicina: serve la prima. */
    @Query("""
            select v from Visita v
            where v.pazienteId = :pazienteId
              and (v.dataVisita < :data
                   or (v.dataVisita = :data and v.creatoIl < :creatoIl))
            order by v.dataVisita desc, v.creatoIl desc
            """)
    List<Visita> precedentiDi(@Param("pazienteId") UUID pazienteId, @Param("data") LocalDate data,
                              @Param("creatoIl") java.time.Instant creatoIl, Pageable pageable);

    List<Visita> findAllByPazienteIdIn(List<UUID> pazienteIds);
}
