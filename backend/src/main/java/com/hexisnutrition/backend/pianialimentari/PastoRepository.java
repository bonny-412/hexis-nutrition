package com.hexisnutrition.backend.pianialimentari;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PastoRepository extends JpaRepository<Pasto, UUID> {
    List<Pasto> findAllByPianoIdOrderByGiornoSettimanaAscOrdineAsc(UUID pianoId);

    void deleteAllByPianoId(UUID pianoId);
}
