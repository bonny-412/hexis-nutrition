package com.hexisnutrition.backend.pazienti;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "plicometrie")
public class Plicometria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "visita_id", nullable = false, unique = true)
    private UUID visitaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProtocolloPlicometrico protocollo;

    @Column(name = "formula_versione", nullable = false)
    private String formulaVersione;

    @Column(name = "eta_anni", nullable = false)
    private Integer etaAnni;

    @Column(name = "coefficiente_c")
    private BigDecimal coefficienteC;

    @Column(name = "coefficiente_m")
    private BigDecimal coefficienteM;

    @Enumerated(EnumType.STRING)
    @Column(name = "etnia_atleta")
    private EtniaAtleta etniaAtleta;

    @Column(name = "plica_pettorale_mm")
    private BigDecimal plicaPettoraleMm;

    @Column(name = "plica_ascellare_mm")
    private BigDecimal plicaAscellareMm;

    @Column(name = "plica_tricipitale_mm")
    private BigDecimal plicaTricipitaleMm;

    @Column(name = "plica_bicipitale_mm")
    private BigDecimal plicaBicipitaleMm;

    @Column(name = "plica_sottoscapolare_mm")
    private BigDecimal plicaSottoscapolareMm;

    @Column(name = "plica_soprailiaca_mm")
    private BigDecimal plicaSoprailiacaMm;

    @Column(name = "plica_addominale_mm")
    private BigDecimal plicaAddominaleMm;

    @Column(name = "plica_coscia_mm")
    private BigDecimal plicaCosciaMm;

    @Column(name = "plica_polpaccio_mm")
    private BigDecimal plicaPolpaccioMm;

    @Column(name = "somma_pliche_mm", nullable = false)
    private BigDecimal sommaPlicheMm;

    @Column(name = "densita_corporea")
    private BigDecimal densitaCorporea;

    @Column(name = "percentuale_grasso", nullable = false)
    private BigDecimal percentualeGrasso;

    @Column(name = "massa_grassa_kg", nullable = false)
    private BigDecimal massaGrassaKg;

    @Column(name = "massa_magra_kg", nullable = false)
    private BigDecimal massaMagraKg;

    @Column(nullable = false)
    private BigDecimal fmi;

    @Column(nullable = false)
    private BigDecimal ffmi;

    @Column(name = "limite_sicurezza_applicato", nullable = false)
    private boolean limiteSicurezzaApplicato;

    @Column(name = "creato_il", nullable = false)
    private Instant creatoIl = Instant.now();

    protected Plicometria() {
    }

    public Plicometria(UUID visitaId, ProtocolloPlicometrico protocollo, String formulaVersione, Integer etaAnni,
                        BigDecimal coefficienteC, BigDecimal coefficienteM, EtniaAtleta etniaAtleta,
                        BigDecimal plicaPettoraleMm, BigDecimal plicaAscellareMm, BigDecimal plicaTricipitaleMm,
                        BigDecimal plicaBicipitaleMm, BigDecimal plicaSottoscapolareMm, BigDecimal plicaSoprailiacaMm,
                        BigDecimal plicaAddominaleMm, BigDecimal plicaCosciaMm, BigDecimal plicaPolpaccioMm,
                        BigDecimal sommaPlicheMm, BigDecimal densitaCorporea, BigDecimal percentualeGrasso,
                        BigDecimal massaGrassaKg, BigDecimal massaMagraKg, BigDecimal fmi, BigDecimal ffmi,
                        boolean limiteSicurezzaApplicato) {
        this.visitaId = visitaId;
        this.protocollo = protocollo;
        this.formulaVersione = formulaVersione;
        this.etaAnni = etaAnni;
        this.coefficienteC = coefficienteC;
        this.coefficienteM = coefficienteM;
        this.etniaAtleta = etniaAtleta;
        this.plicaPettoraleMm = plicaPettoraleMm;
        this.plicaAscellareMm = plicaAscellareMm;
        this.plicaTricipitaleMm = plicaTricipitaleMm;
        this.plicaBicipitaleMm = plicaBicipitaleMm;
        this.plicaSottoscapolareMm = plicaSottoscapolareMm;
        this.plicaSoprailiacaMm = plicaSoprailiacaMm;
        this.plicaAddominaleMm = plicaAddominaleMm;
        this.plicaCosciaMm = plicaCosciaMm;
        this.plicaPolpaccioMm = plicaPolpaccioMm;
        this.sommaPlicheMm = sommaPlicheMm;
        this.densitaCorporea = densitaCorporea;
        this.percentualeGrasso = percentualeGrasso;
        this.massaGrassaKg = massaGrassaKg;
        this.massaMagraKg = massaMagraKg;
        this.fmi = fmi;
        this.ffmi = ffmi;
        this.limiteSicurezzaApplicato = limiteSicurezzaApplicato;
    }

    public UUID getId() {
        return id;
    }

    public UUID getVisitaId() {
        return visitaId;
    }

    public ProtocolloPlicometrico getProtocollo() {
        return protocollo;
    }

    public String getFormulaVersione() {
        return formulaVersione;
    }

    public Integer getEtaAnni() {
        return etaAnni;
    }

    public BigDecimal getCoefficienteC() {
        return coefficienteC;
    }

    public BigDecimal getCoefficienteM() {
        return coefficienteM;
    }

    public EtniaAtleta getEtniaAtleta() {
        return etniaAtleta;
    }

    public BigDecimal getPlicaPettoraleMm() {
        return plicaPettoraleMm;
    }

    public BigDecimal getPlicaAscellareMm() {
        return plicaAscellareMm;
    }

    public BigDecimal getPlicaTricipitaleMm() {
        return plicaTricipitaleMm;
    }

    public BigDecimal getPlicaBicipitaleMm() {
        return plicaBicipitaleMm;
    }

    public BigDecimal getPlicaSottoscapolareMm() {
        return plicaSottoscapolareMm;
    }

    public BigDecimal getPlicaSoprailiacaMm() {
        return plicaSoprailiacaMm;
    }

    public BigDecimal getPlicaAddominaleMm() {
        return plicaAddominaleMm;
    }

    public BigDecimal getPlicaCosciaMm() {
        return plicaCosciaMm;
    }

    public BigDecimal getPlicaPolpaccioMm() {
        return plicaPolpaccioMm;
    }

    public BigDecimal getSommaPlicheMm() {
        return sommaPlicheMm;
    }

    public BigDecimal getDensitaCorporea() {
        return densitaCorporea;
    }

    public BigDecimal getPercentualeGrasso() {
        return percentualeGrasso;
    }

    public BigDecimal getMassaGrassaKg() {
        return massaGrassaKg;
    }

    public BigDecimal getMassaMagraKg() {
        return massaMagraKg;
    }

    public BigDecimal getFmi() {
        return fmi;
    }

    public BigDecimal getFfmi() {
        return ffmi;
    }

    public boolean isLimiteSicurezzaApplicato() {
        return limiteSicurezzaApplicato;
    }
}
