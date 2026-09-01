package com.hexisnutrition.backend.pazienti;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PlicometriaRepository extends JpaRepository<Plicometria, UUID> {
}
