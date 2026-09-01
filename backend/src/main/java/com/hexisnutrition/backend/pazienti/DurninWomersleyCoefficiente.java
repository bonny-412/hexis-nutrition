package com.hexisnutrition.backend.pazienti;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "durnin_womersley_coefficienti")
public class DurninWomersleyCoefficiente {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sesso sesso;

    @Column(name = "eta_min", nullable = false)
    private Integer etaMin;

    @Column(name = "eta_max")
    private Integer etaMax;

    @Column(nullable = false)
    private BigDecimal c;

    @Column(nullable = false)
    private BigDecimal m;

    protected DurninWomersleyCoefficiente() {
    }

    public DurninWomersleyCoefficiente(Sesso sesso, Integer etaMin, Integer etaMax, BigDecimal c, BigDecimal m) {
        this.id = UUID.randomUUID();
        this.sesso = sesso;
        this.etaMin = etaMin;
        this.etaMax = etaMax;
        this.c = c;
        this.m = m;
    }

    public Sesso getSesso() {
        return sesso;
    }

    public Integer getEtaMin() {
        return etaMin;
    }

    public Integer getEtaMax() {
        return etaMax;
    }

    public BigDecimal getC() {
        return c;
    }

    public BigDecimal getM() {
        return m;
    }
}
