package com.hexisnutrition.backend.alimenti;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface AlimentoRepository extends JpaRepository<Alimento, UUID>, JpaSpecificationExecutor<Alimento> {
}
