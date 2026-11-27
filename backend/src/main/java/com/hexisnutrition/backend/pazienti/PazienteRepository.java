package com.hexisnutrition.backend.pazienti;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PazienteRepository extends JpaRepository<Paziente, UUID>, JpaSpecificationExecutor<Paziente> {
    List<Paziente> findAllByProfessionistaId(UUID professionistaId);

    long countByProfessionistaIdAndArchiviato(UUID professionistaId, boolean archiviato);

    Optional<Paziente> findByEmailAndStatoAccount(String email, StatoAccountPaziente statoAccount);

    @Query("select p.versioneSessione from Paziente p where p.id = :id")
    Optional<Integer> versioneSessione(@Param("id") UUID id);

    boolean existsByEmailAndStatoAccount(String email, StatoAccountPaziente statoAccount);
}
