package com.hexisnutrition.backend.pazienti;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DurninWomersleyCoefficienteRepository extends JpaRepository<DurninWomersleyCoefficiente, UUID> {

    @Query("""
            SELECT c FROM DurninWomersleyCoefficiente c
            WHERE c.sesso = :sesso
              AND c.etaMin <= :eta
              AND (c.etaMax IS NULL OR c.etaMax >= :eta)
            ORDER BY c.etaMin DESC
            """)
    List<DurninWomersleyCoefficiente> trovaCandidati(@Param("sesso") Sesso sesso, @Param("eta") Integer eta);
}
