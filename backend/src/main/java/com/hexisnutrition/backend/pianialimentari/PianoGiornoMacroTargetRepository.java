package com.hexisnutrition.backend.pianialimentari;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PianoGiornoMacroTargetRepository extends JpaRepository<PianoGiornoMacroTarget, UUID> {
    List<PianoGiornoMacroTarget> findAllByPianoId(UUID pianoId);

    void deleteAllByPianoId(UUID pianoId);
}
