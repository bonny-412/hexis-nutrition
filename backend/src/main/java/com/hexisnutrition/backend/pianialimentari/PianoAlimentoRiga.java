package com.hexisnutrition.backend.pianialimentari;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "piano_alimento_righe")
public class PianoAlimentoRiga {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "pasto_id")
    private UUID pastoId;

    @Column(name = "esempio_id")
    private UUID esempioId;

    @Column(name = "alimento_id")
    private UUID alimentoId;

    @Column(nullable = false)
    private String nome;

    @Column(name = "kcal_100g", nullable = false)
    private BigDecimal kcal100g;

    @Column(name = "proteine_100g", nullable = false)
    private BigDecimal proteine100g;

    @Column(name = "carboidrati_100g", nullable = false)
    private BigDecimal carboidrati100g;

    @Column(name = "grassi_100g", nullable = false)
    private BigDecimal grassi100g;

    @Column(name = "zuccheri_100g")
    private BigDecimal zuccheri100g;

    @Column(nullable = false)
    private BigDecimal grammi;

    @Column(nullable = false)
    private int ordine;

    protected PianoAlimentoRiga() {
    }

    public static PianoAlimentoRiga perPasto(UUID pastoId, UUID alimentoId, String nome, BigDecimal kcal100g,
            BigDecimal proteine100g, BigDecimal carboidrati100g, BigDecimal grassi100g, BigDecimal zuccheri100g,
            BigDecimal grammi, int ordine) {
        PianoAlimentoRiga riga = new PianoAlimentoRiga();
        riga.pastoId = pastoId;
        riga.alimentoId = alimentoId;
        riga.nome = nome;
        riga.kcal100g = kcal100g;
        riga.proteine100g = proteine100g;
        riga.carboidrati100g = carboidrati100g;
        riga.grassi100g = grassi100g;
        riga.zuccheri100g = zuccheri100g;
        riga.grammi = grammi;
        riga.ordine = ordine;
        return riga;
    }

    public static PianoAlimentoRiga perEsempio(UUID esempioId, UUID alimentoId, String nome, BigDecimal kcal100g,
            BigDecimal proteine100g, BigDecimal carboidrati100g, BigDecimal grassi100g, BigDecimal zuccheri100g,
            BigDecimal grammi, int ordine) {
        PianoAlimentoRiga riga = new PianoAlimentoRiga();
        riga.esempioId = esempioId;
        riga.alimentoId = alimentoId;
        riga.nome = nome;
        riga.kcal100g = kcal100g;
        riga.proteine100g = proteine100g;
        riga.carboidrati100g = carboidrati100g;
        riga.grassi100g = grassi100g;
        riga.zuccheri100g = zuccheri100g;
        riga.grammi = grammi;
        riga.ordine = ordine;
        return riga;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPastoId() {
        return pastoId;
    }

    public UUID getEsempioId() {
        return esempioId;
    }

    public UUID getAlimentoId() {
        return alimentoId;
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getKcal100g() {
        return kcal100g;
    }

    public BigDecimal getProteine100g() {
        return proteine100g;
    }

    public BigDecimal getCarboidrati100g() {
        return carboidrati100g;
    }

    public BigDecimal getGrassi100g() {
        return grassi100g;
    }

    public BigDecimal getZuccheri100g() {
        return zuccheri100g;
    }

    public BigDecimal getGrammi() {
        return grammi;
    }

    public int getOrdine() {
        return ordine;
    }
}
