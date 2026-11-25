package com.hexisnutrition.backend.alimenti;

import jakarta.persistence.criteria.Expression;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class AlimentoSpecifications {

    private AlimentoSpecifications() {
    }

    public static Specification<Alimento> visibilePer(UUID professionistaId) {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get("professionistaId")),
                cb.equal(root.get("professionistaId"), professionistaId));
    }

    public static Specification<Alimento> conRicerca(String ricerca) {
        String pattern = "%" + ricerca.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("nome")), pattern),
                cb.like(cb.lower(root.get("categoria")), pattern));
    }

    /**
     * Ordina per pertinenza rispetto alla ricerca ("pasta" → prima le paste, non Focaccia o Friselle trovate per categoria):
     * 0 nome che inizia col testo, 1 una parola del nome che inizia col testo, 2 testo dentro il nome, 3 solo categoria.
     * Non filtra nulla. Nella query di conteggio l'ordine non serve (e non è ammesso), quindi lì non si imposta.
     */
    public static Specification<Alimento> ordinataPerPertinenza(String ricerca) {
        String testo = ricerca.toLowerCase();
        return (root, query, cb) -> {
            if (!Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                Expression<String> nome = cb.lower(root.get("nome"));
                Expression<Integer> punteggio = cb.<Integer>selectCase()
                        .when(cb.like(nome, testo + "%"), 0)
                        .when(cb.or(
                                cb.like(nome, "% " + testo + "%"),
                                cb.like(nome, "%," + testo + "%"),
                                cb.like(nome, "%(" + testo + "%")), 1)
                        .when(cb.like(nome, "%" + testo + "%"), 2)
                        .otherwise(3);
                query.orderBy(cb.asc(punteggio), cb.asc(root.get("nome")));
            }
            return cb.conjunction();
        };
    }

    /** Ordine per nome quando "pertinenza" è chiesta senza testo di ricerca. */
    public static Specification<Alimento> ordinataPerNome() {
        return (root, query, cb) -> {
            if (!Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                query.orderBy(cb.asc(root.get("nome")));
            }
            return cb.conjunction();
        };
    }

    public static Specification<Alimento> soloBda() {
        return (root, query, cb) -> cb.isNull(root.get("professionistaId"));
    }

    public static Specification<Alimento> soloPersonalizzati() {
        return (root, query, cb) -> cb.isNotNull(root.get("professionistaId"));
    }
}
