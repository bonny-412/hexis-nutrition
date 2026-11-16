package com.hexisnutrition.backend.notifiche;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificaRepository extends JpaRepository<Notifica, UUID> {

    List<Notifica> findAllByProfessionistaId(UUID professionistaId);

    void deleteByAppuntamentoId(UUID appuntamentoId);

    void deleteByAppuntamentoIdAndScattaIlNot(UUID appuntamentoId, Instant scattaIl);

    void deleteByScattaIlBefore(Instant limite);

    /** Restituisce 1 se la riga è stata inserita, 0 se esisteva già per lo stesso (appuntamento, scatto). */
    @Modifying
    @Query(value = """
            INSERT INTO notifiche (id, professionista_id, appuntamento_id, scatta_il)
            VALUES (:id, :professionistaId, :appuntamentoId, :scattaIl)
            ON CONFLICT (appuntamento_id, scatta_il) DO NOTHING
            """, nativeQuery = true)
    int inserisciSeAssente(@Param("id") UUID id,
                           @Param("professionistaId") UUID professionistaId,
                           @Param("appuntamentoId") UUID appuntamentoId,
                           @Param("scattaIl") Instant scattaIl);
}
