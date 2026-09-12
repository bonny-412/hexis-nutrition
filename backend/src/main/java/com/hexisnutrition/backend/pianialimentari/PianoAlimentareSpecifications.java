package com.hexisnutrition.backend.pianialimentari;

import com.hexisnutrition.backend.pazienti.Paziente;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

public final class PianoAlimentareSpecifications {

    private PianoAlimentareSpecifications() {
    }

    public static Specification<PianoAlimentare> delProfessionista(UUID professionistaId) {
        return (root, query, cb) -> cb.equal(root.get("professionistaId"), professionistaId);
    }

    public static Specification<PianoAlimentare> delPaziente(UUID pazienteId) {
        return (root, query, cb) -> cb.equal(root.get("pazienteId"), pazienteId);
    }

    /** Match sul nome del piano o su nome/cognome del paziente collegato (nessuna relazione JPA: subquery correlata). */
    public static Specification<PianoAlimentare> conRicerca(String ricerca) {
        String pattern = "%" + ricerca.toLowerCase() + "%";
        return (root, query, cb) -> {
            Subquery<UUID> pazienteConNome = query.subquery(UUID.class);
            var pazienteRoot = pazienteConNome.from(Paziente.class);
            pazienteConNome.select(pazienteRoot.get("id"))
                    .where(cb.and(
                            cb.equal(pazienteRoot.get("id"), root.get("pazienteId")),
                            cb.or(
                                    cb.like(cb.lower(pazienteRoot.get("nome")), pattern),
                                    cb.like(cb.lower(pazienteRoot.get("cognome")), pattern))));
            return cb.or(cb.like(cb.lower(root.get("nome")), pattern), cb.exists(pazienteConNome));
        };
    }

    public static Specification<PianoAlimentare> conStato(StatoPianoVisualizzato stato) {
        LocalDate oggi = LocalDate.now();
        return switch (stato) {
            case BOZZA -> (root, query, cb) -> cb.equal(root.get("stato"), StatoPiano.BOZZA);
            case TERMINATO -> (root, query, cb) -> cb.equal(root.get("stato"), StatoPiano.TERMINATO);
            case ATTIVO -> (root, query, cb) -> cb.and(
                    cb.equal(root.get("stato"), StatoPiano.ATTIVO),
                    cb.or(cb.isNull(root.get("dataFine")), cb.greaterThanOrEqualTo(root.get("dataFine"), oggi)));
            case SCADUTO -> (root, query, cb) -> cb.and(
                    cb.equal(root.get("stato"), StatoPiano.ATTIVO),
                    cb.isNotNull(root.get("dataFine")),
                    cb.lessThan(root.get("dataFine"), oggi));
        };
    }
}
