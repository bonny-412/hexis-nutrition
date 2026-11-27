package com.hexisnutrition.backend.professionisti;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProfessionistaRepository extends JpaRepository<Professionista, UUID> {
    Optional<Professionista> findByEmail(String email);

    @Query("select p.versioneSessione from Professionista p where p.id = :id")
    Optional<Integer> versioneSessione(@Param("id") UUID id);
}
