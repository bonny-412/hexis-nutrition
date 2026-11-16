package com.hexisnutrition.backend.agenda;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AppuntamentoRepository extends JpaRepository<Appuntamento, UUID> {

    List<Appuntamento> findAllByProfessionistaIdAndDataBetweenOrderByDataAscOraInizioAsc(
            UUID professionistaId, LocalDate da, LocalDate a);

    /** Tutti i professionisti: serve allo scheduler delle notifiche. */
    List<Appuntamento> findAllByDataBetweenAndNotificaTrue(LocalDate da, LocalDate a);

    List<Appuntamento> findAllByProfessionistaIdAndData(UUID professionistaId, LocalDate data);

    List<Appuntamento> findAllByPazienteIdInAndStatoAndDataGreaterThanEqual(
            Collection<UUID> pazienteIds, StatoAppuntamento stato, LocalDate da);

    @Modifying
    @Query("""
            delete from Appuntamento a
            where a.pazienteId = :pazienteId
              and (
                (a.tipo in (com.hexisnutrition.backend.agenda.TipoAppuntamento.VISITA_STUDIO,
                            com.hexisnutrition.backend.agenda.TipoAppuntamento.VIDEOCONSULTO)
                 and a.stato = com.hexisnutrition.backend.agenda.StatoAppuntamento.PROGRAMMATA)
                or a.tipo = com.hexisnutrition.backend.agenda.TipoAppuntamento.PROMEMORIA
              )
              and (
                a.data > :oggi
                or (a.data = :oggi and (a.oraInizio is null or a.oraInizio >= :ora))
              )
            """)
    void eliminaFuturiDelPaziente(@Param("pazienteId") UUID pazienteId,
                                  @Param("oggi") LocalDate oggi,
                                  @Param("ora") LocalTime ora);
}
