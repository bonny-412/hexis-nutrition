package com.hexisnutrition.backend.professionisti;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProfessionistaRepository extends JpaRepository<Professionista, UUID> {
    Optional<Professionista> findByEmail(String email);
}
