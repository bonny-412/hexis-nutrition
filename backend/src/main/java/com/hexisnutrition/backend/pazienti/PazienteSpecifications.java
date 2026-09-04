package com.hexisnutrition.backend.pazienti;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

public final class PazienteSpecifications {

    private PazienteSpecifications() {
    }

    /** Nessuna relazione JPA tra Paziente e Visita (FK grezza): l'ultima visita è quella con `dataVisita` massima per quel paziente. */
    private static Subquery<LocalDate> dataUltimaVisita(Root<Paziente> paziente, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Subquery<LocalDate> sub = query.subquery(LocalDate.class);
        var visita = sub.from(Visita.class);
        sub.select(cb.greatest(visita.<LocalDate>get("dataVisita")))
                .where(cb.equal(visita.get("pazienteId"), paziente.get("id")));
        return sub;
    }

    public static Specification<Paziente> delProfessionista(UUID professionistaId) {
        return (root, query, cb) -> cb.equal(root.get("professionistaId"), professionistaId);
    }

    public static Specification<Paziente> conArchiviato(boolean archiviato) {
        return (root, query, cb) -> cb.equal(root.get("archiviato"), archiviato);
    }

    public static Specification<Paziente> conRicerca(String ricerca) {
        String pattern = "%" + ricerca.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("nome")), pattern),
                cb.like(cb.lower(root.get("cognome")), pattern),
                cb.like(cb.lower(root.get("email")), pattern),
                cb.like(cb.lower(root.get("codiceFiscale")), pattern));
    }

    public static Specification<Paziente> conStatoAccount(StatoAccountPaziente statoAccount) {
        return (root, query, cb) -> cb.equal(root.get("statoAccount"), statoAccount);
    }

    public static Specification<Paziente> conObiettivoUltimaVisita(ObiettivoVisita obiettivo) {
        return (root, query, cb) -> {
            Subquery<UUID> visitaConObiettivo = query.subquery(UUID.class);
            var visita = visitaConObiettivo.from(Visita.class);
            visitaConObiettivo.select(visita.get("id"))
                    .where(cb.and(
                            cb.equal(visita.get("pazienteId"), root.get("id")),
                            cb.equal(visita.get("obiettivo"), obiettivo),
                            cb.equal(visita.get("dataVisita"), dataUltimaVisita(root, query, cb))));

            return cb.exists(visitaConObiettivo);
        };
    }

    /** Nullo per i pazienti senza visite: correttamente esclusi quando questo filtro è attivo. */
    public static Specification<Paziente> conDataUltimaVisitaTra(LocalDate da, LocalDate a) {
        return (root, query, cb) -> {
            Subquery<LocalDate> dataUltimaVisita = dataUltimaVisita(root, query, cb);
            if (da != null && a != null) {
                return cb.between(dataUltimaVisita, da, a);
            }
            if (da != null) {
                return cb.greaterThanOrEqualTo(dataUltimaVisita, da);
            }
            if (a != null) {
                return cb.lessThanOrEqualTo(dataUltimaVisita, a);
            }
            return cb.conjunction();
        };
    }
}
