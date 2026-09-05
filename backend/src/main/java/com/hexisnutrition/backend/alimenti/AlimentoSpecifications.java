package com.hexisnutrition.backend.alimenti;

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

    public static Specification<Alimento> soloBda() {
        return (root, query, cb) -> cb.isNull(root.get("professionistaId"));
    }

    public static Specification<Alimento> soloPersonalizzati() {
        return (root, query, cb) -> cb.isNotNull(root.get("professionistaId"));
    }
}
