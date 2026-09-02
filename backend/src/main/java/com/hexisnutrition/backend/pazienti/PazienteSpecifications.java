package com.hexisnutrition.backend.pazienti;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

public final class PazienteSpecifications {

    private PazienteSpecifications() {
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

    public static Specification<Paziente> conSesso(Sesso sesso) {
        return (root, query, cb) -> cb.equal(root.get("sesso"), sesso);
    }

    public static Specification<Paziente> conDataNascitaTra(LocalDate da, LocalDate a) {
        return (root, query, cb) -> {
            if (da != null && a != null) {
                return cb.between(root.get("dataNascita"), da, a);
            }
            if (da != null) {
                return cb.greaterThanOrEqualTo(root.get("dataNascita"), da);
            }
            if (a != null) {
                return cb.lessThanOrEqualTo(root.get("dataNascita"), a);
            }
            return cb.conjunction();
        };
    }
}
