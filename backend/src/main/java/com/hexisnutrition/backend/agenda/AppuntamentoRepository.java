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

    /** Appuntamenti del periodo; i blocchi (multi-giorno) compaiono se il loro intervallo tocca il periodo. */
    @Query("""
            select a from Appuntamento a
            where a.professionistaId = :professionistaId
              and (
                (a.tipo <> com.hexisnutrition.backend.agenda.TipoAppuntamento.BLOCCO
                 and a.data between :da and :a)
                or (a.tipo = com.hexisnutrition.backend.agenda.TipoAppuntamento.BLOCCO
                    and a.data <= :a and a.dataFine >= :da)
              )
            order by a.data asc, a.oraInizio asc
            """)
    List<Appuntamento> elencoNelPeriodo(@Param("professionistaId") UUID professionistaId,
                                        @Param("da") LocalDate da,
                                        @Param("a") LocalDate a);

    /** Blocchi del professionista il cui intervallo [data, dataFine] contiene il giorno. */
    @Query("""
            select a from Appuntamento a
            where a.professionistaId = :professionistaId
              and a.tipo = com.hexisnutrition.backend.agenda.TipoAppuntamento.BLOCCO
              and a.data <= :giorno and a.dataFine >= :giorno
            """)
    List<Appuntamento> blocchiCheCopronoIlGiorno(@Param("professionistaId") UUID professionistaId,
                                                 @Param("giorno") LocalDate giorno);

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

    /**
     * Lock di transazione (PostgreSQL advisory lock) sull'agenda di un professionista: le scritture
     * che controllano le sovrapposizioni passano una alla volta, così due visite create nello stesso
     * istante non superano entrambe il controllo. Si rilascia da solo a fine transazione.
     */
    @Query(value = "select 1 from (select pg_advisory_xact_lock(hashtextextended(cast(:chiave as text), 0))) l",
            nativeQuery = true)
    Integer bloccaAgenda(@Param("chiave") String chiave);
}
