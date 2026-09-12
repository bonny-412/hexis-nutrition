package com.hexisnutrition.backend.pianialimentari;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PianoAlimentoRigaRepository extends JpaRepository<PianoAlimentoRiga, UUID> {
    List<PianoAlimentoRiga> findAllByPastoIdInOrderByOrdineAsc(List<UUID> pastoIds);

    List<PianoAlimentoRiga> findAllByEsempioIdInOrderByOrdineAsc(List<UUID> esempioIds);

    void deleteAllByPastoIdIn(List<UUID> pastoIds);

    void deleteAllByEsempioIdIn(List<UUID> esempioIds);
}
