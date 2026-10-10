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

    /** Esclude i piani di pazienti archiviati (nessuna relazione JPA: subquery correlata come in conRicerca). */
    public static Specification<PianoAlimentare> diPazientiNonArchiviati() {
        return (root, query, cb) -> {
            Subquery<UUID> pazienteAttivo = query.subquery(UUID.class);
            var pazienteRoot = pazienteAttivo.from(Paziente.class);
            pazienteAttivo.select(pazienteRoot.get("id"))
                    .where(cb.and(
                            cb.equal(pazienteRoot.get("id"), root.get("pazienteId")),
                            cb.isFalse(pazienteRoot.get("archiviato"))));
            return cb.exists(pazienteAttivo);
        };
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

    /** Tutti i piani del paziente tranne quello effettivamente attivo (gli scaduti restano inclusi). */
    public static Specification<PianoAlimentare> nonAttivo() {
        return Specification.not(conStato(StatoPianoVisualizzato.ATTIVO));
    }

    public static Specification<PianoAlimentare> nonBozza() {
        return Specification.not(conStato(StatoPianoVisualizzato.BOZZA));
    }

    public static Specification<PianoAlimentare> conDataInizioTra(LocalDate da, LocalDate a) {
        return conDataTra("dataInizio", da, a);
    }

    /** I piani senza data di fine (nullable) sono esclusi quando questo filtro è attivo. */
    public static Specification<PianoAlimentare> conDataFineTra(LocalDate da, LocalDate a) {
        return conDataTra("dataFine", da, a);
    }

    /** Estremi inclusivi; un estremo nullo significa nessun limite da quel lato. */
    private static Specification<PianoAlimentare> conDataTra(String campo, LocalDate da, LocalDate a) {
        return (root, query, cb) -> {
            if (da != null && a != null) {
                return cb.between(root.get(campo), da, a);
            }
            if (da != null) {
                return cb.greaterThanOrEqualTo(root.get(campo), da);
            }
            if (a != null) {
                return cb.lessThanOrEqualTo(root.get(campo), a);
            }
            return cb.conjunction();
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
