package com.hexisnutrition.backend.pianialimentari;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PianoNotaRepository extends JpaRepository<PianoNota, UUID> {
    List<PianoNota> findAllByPianoIdOrderByDataDescCreatoIlDesc(UUID pianoId);

    Optional<PianoNota> findByIdAndPianoId(UUID id, UUID pianoId);
}
