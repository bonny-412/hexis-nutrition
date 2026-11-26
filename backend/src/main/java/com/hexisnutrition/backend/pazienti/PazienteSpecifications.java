package com.hexisnutrition.backend.pazienti;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import com.hexisnutrition.backend.agenda.AgendaTempo;
import com.hexisnutrition.backend.agenda.Appuntamento;
import com.hexisnutrition.backend.agenda.StatoAppuntamento;
import com.hexisnutrition.backend.agenda.TipoAppuntamento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    /**
     * Nome, cognome e codice fiscale combaciano in qualunque punto; l'email solo dall'inizio:
     * cercando ".com" o "gmail" combacerebbero quasi tutti i pazienti, cercando "mario" si trova ancora mario.rossi@...
     */
    public static Specification<Paziente> conRicerca(String ricerca) {
        String minuscolo = ricerca.toLowerCase();
        String pattern = "%" + minuscolo + "%";
        String inizio = minuscolo + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("nome")), pattern),
                cb.like(cb.lower(root.get("cognome")), pattern),
                cb.like(cb.lower(root.get("email")), inizio),
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

    /**
     * Pazienti con almeno una visita PROGRAMMATA (studio/videoconsulto) tra adesso e la fine del
     * giorno oggi + giorni (estremo incluso).
     */
    public static Specification<Paziente> conProssimaVisitaEntro(int giorni) {
        return conProssimaVisita(giorni);
    }

    public static Specification<Paziente> senzaProssimaVisita() {
        return Specification.not(conProssimaVisita(null));
    }

    private static Specification<Paziente> conProssimaVisita(Integer giorni) {
        return (root, query, cb) -> {
            LocalDateTime adesso = AgendaTempo.adesso();
            LocalDate oggi = adesso.toLocalDate();
            Subquery<UUID> sub = query.subquery(UUID.class);
            var app = sub.from(Appuntamento.class);
            sub.select(app.<UUID>get("id")).where(cb.and(
                    cb.equal(app.get("pazienteId"), root.get("id")),
                    cb.equal(app.get("stato"), StatoAppuntamento.PROGRAMMATA),
                    app.get("tipo").in(TipoAppuntamento.VISITA_STUDIO, TipoAppuntamento.VIDEOCONSULTO),
                    cb.isNotNull(app.get("oraInizio")),
                    giorni == null ? cb.conjunction() : cb.lessThanOrEqualTo(app.<LocalDate>get("data"), oggi.plusDays(giorni)),
                    cb.or(
                            cb.greaterThan(app.<LocalDate>get("data"), oggi),
                            cb.and(cb.equal(app.get("data"), oggi),
                                    cb.greaterThanOrEqualTo(app.<LocalTime>get("oraInizio"), adesso.toLocalTime())))));
            return cb.exists(sub);
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
