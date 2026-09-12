package com.hexisnutrition.backend.pianialimentari;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PianoEsempioRepository extends JpaRepository<PianoEsempio, UUID> {
    List<PianoEsempio> findAllByPianoIdOrderByTipoPastoAscOrdineAsc(UUID pianoId);

    void deleteAllByPianoId(UUID pianoId);
}
