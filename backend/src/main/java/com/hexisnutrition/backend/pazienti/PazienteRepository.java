package com.hexisnutrition.backend.pazienti;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PazienteRepository extends JpaRepository<Paziente, UUID> {
    List<Paziente> findAllByProfessionistaId(UUID professionistaId);

    Optional<Paziente> findByEmailAndStatoAccount(String email, StatoAccountPaziente statoAccount);

    boolean existsByEmailAndStatoAccount(String email, StatoAccountPaziente statoAccount);
}
