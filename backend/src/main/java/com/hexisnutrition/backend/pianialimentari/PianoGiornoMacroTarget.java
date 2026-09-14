package com.hexisnutrition.backend.pianialimentari;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "piano_giorno_macro_target")
public class PianoGiornoMacroTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "piano_id", nullable = false)
    private UUID pianoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "giorno_settimana", nullable = false)
    private GiornoSettimana giornoSettimana;

    @Column(name = "kcal_target")
    private BigDecimal kcalTarget;

    @Column(name = "proteine_target")
    private BigDecimal proteineTarget;

    @Column(name = "carboidrati_target")
    private BigDecimal carboidratiTarget;

    @Column(name = "grassi_target")
    private BigDecimal grassiTarget;

    @Column(columnDefinition = "TEXT")
    private String nota;

    protected PianoGiornoMacroTarget() {
    }

    public PianoGiornoMacroTarget(UUID pianoId, GiornoSettimana giornoSettimana, BigDecimal kcalTarget,
                                   BigDecimal proteineTarget, BigDecimal carboidratiTarget, BigDecimal grassiTarget,
                                   String nota) {
        this.pianoId = pianoId;
        this.giornoSettimana = giornoSettimana;
        this.kcalTarget = kcalTarget;
        this.proteineTarget = proteineTarget;
        this.carboidratiTarget = carboidratiTarget;
        this.grassiTarget = grassiTarget;
        this.nota = nota;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPianoId() {
        return pianoId;
    }

    public GiornoSettimana getGiornoSettimana() {
        return giornoSettimana;
    }

    public BigDecimal getKcalTarget() {
        return kcalTarget;
    }

    public BigDecimal getProteineTarget() {
        return proteineTarget;
    }

    public BigDecimal getCarboidratiTarget() {
        return carboidratiTarget;
    }

    public BigDecimal getGrassiTarget() {
        return grassiTarget;
    }

    public String getNota() {
        return nota;
    }
}
