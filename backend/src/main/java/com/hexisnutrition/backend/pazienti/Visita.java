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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "visite")
public class Visita {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "paziente_id", nullable = false)
    private UUID pazienteId;

    @Column(name = "data_visita", nullable = false)
    private LocalDate dataVisita = LocalDate.now();

    @Column(name = "altezza_cm", nullable = false)
    private Integer altezzaCm;

    @Column(name = "peso_kg", nullable = false)
    private BigDecimal pesoKg;

    @Column(name = "circonferenza_vita_cm")
    private BigDecimal circonferenzaVitaCm;

    @Column(name = "circonferenza_fianchi_cm")
    private BigDecimal circonferenzaFianchiCm;

    @Column(name = "circonferenza_addome_cm")
    private BigDecimal circonferenzaAddomeCm;

    @Column(name = "circonferenza_braccio_rilassato_cm")
    private BigDecimal circonferenzaBraccioRilassatoCm;

    @Column(name = "circonferenza_coscia_cm")
    private BigDecimal circonferenzaCosciaCm;

    @Column(name = "circonferenza_polpaccio_cm")
    private BigDecimal circonferenzaPolpaccioCm;

    @Column(name = "circonferenza_collo_cm")
    private BigDecimal circonferenzaColloCm;

    @Column(name = "circonferenza_torace_cm")
    private BigDecimal circonferenzaToraceCm;

    @Column(name = "circonferenza_braccio_contratto_cm")
    private BigDecimal circonferenzaBraccioContrattoCm;

    @Column(name = "circonferenza_avambraccio_cm")
    private BigDecimal circonferenzaAvambraccioCm;

    @Column(name = "circonferenza_caviglia_cm")
    private BigDecimal circonferenzaCavigliaCm;

    @Enumerated(EnumType.STRING)
    @Column(name = "protocollo_vita", nullable = false)
    private ProtocolloVita protocolloVita = ProtocolloVita.OMS;

    @Column(name = "bmi")
    private BigDecimal bmi;

    @Column(name = "whr")
    private BigDecimal whr;

    @Column(name = "whtr")
    private BigDecimal whtr;

    @Column(name = "mamc_cm")
    private BigDecimal mamcCm;

    @Column(name = "creato_il", nullable = false)
    private Instant creatoIl = Instant.now();

    protected Visita() {
    }

    public Visita(UUID pazienteId, LocalDate dataVisita, Integer altezzaCm, BigDecimal pesoKg,
                  BigDecimal circonferenzaVitaCm, BigDecimal circonferenzaFianchiCm,
                  BigDecimal circonferenzaAddomeCm, BigDecimal circonferenzaBraccioRilassatoCm,
                  BigDecimal circonferenzaCosciaCm, BigDecimal circonferenzaPolpaccioCm,
                  BigDecimal circonferenzaColloCm, BigDecimal circonferenzaToraceCm,
                  BigDecimal circonferenzaBraccioContrattoCm, BigDecimal circonferenzaAvambraccioCm,
                  BigDecimal circonferenzaCavigliaCm, ProtocolloVita protocolloVita) {
        this.pazienteId = pazienteId;
        if (dataVisita != null) {
            this.dataVisita = dataVisita;
        }
        this.altezzaCm = altezzaCm;
        this.pesoKg = pesoKg;
        this.circonferenzaVitaCm = circonferenzaVitaCm;
        this.circonferenzaFianchiCm = circonferenzaFianchiCm;
        this.circonferenzaAddomeCm = circonferenzaAddomeCm;
        this.circonferenzaBraccioRilassatoCm = circonferenzaBraccioRilassatoCm;
        this.circonferenzaCosciaCm = circonferenzaCosciaCm;
        this.circonferenzaPolpaccioCm = circonferenzaPolpaccioCm;
        this.circonferenzaColloCm = circonferenzaColloCm;
        this.circonferenzaToraceCm = circonferenzaToraceCm;
        this.circonferenzaBraccioContrattoCm = circonferenzaBraccioContrattoCm;
        this.circonferenzaAvambraccioCm = circonferenzaAvambraccioCm;
        this.circonferenzaCavigliaCm = circonferenzaCavigliaCm;
        if (protocolloVita != null) {
            this.protocolloVita = protocolloVita;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getPazienteId() {
        return pazienteId;
    }

    public LocalDate getDataVisita() {
        return dataVisita;
    }

    public Integer getAltezzaCm() {
        return altezzaCm;
    }

    public BigDecimal getPesoKg() {
        return pesoKg;
    }

    public BigDecimal getCirconferenzaVitaCm() {
        return circonferenzaVitaCm;
    }

    public BigDecimal getCirconferenzaFianchiCm() {
        return circonferenzaFianchiCm;
    }

    public BigDecimal getCirconferenzaAddomeCm() {
        return circonferenzaAddomeCm;
    }

    public BigDecimal getCirconferenzaBraccioRilassatoCm() {
        return circonferenzaBraccioRilassatoCm;
    }

    public BigDecimal getCirconferenzaCosciaCm() {
        return circonferenzaCosciaCm;
    }

    public BigDecimal getCirconferenzaPolpaccioCm() {
        return circonferenzaPolpaccioCm;
    }

    public BigDecimal getCirconferenzaColloCm() {
        return circonferenzaColloCm;
    }

    public BigDecimal getCirconferenzaToraceCm() {
        return circonferenzaToraceCm;
    }

    public BigDecimal getCirconferenzaBraccioContrattoCm() {
        return circonferenzaBraccioContrattoCm;
    }

    public BigDecimal getCirconferenzaAvambraccioCm() {
        return circonferenzaAvambraccioCm;
    }

    public BigDecimal getCirconferenzaCavigliaCm() {
        return circonferenzaCavigliaCm;
    }

    public ProtocolloVita getProtocolloVita() {
        return protocolloVita;
    }

    public BigDecimal getBmi() {
        return bmi;
    }

    public void setBmi(BigDecimal bmi) {
        this.bmi = bmi;
    }

    public BigDecimal getWhr() {
        return whr;
    }

    public void setWhr(BigDecimal whr) {
        this.whr = whr;
    }

    public BigDecimal getWhtr() {
        return whtr;
    }

    public void setWhtr(BigDecimal whtr) {
        this.whtr = whtr;
    }

    public BigDecimal getMamcCm() {
        return mamcCm;
    }

    public void setMamcCm(BigDecimal mamcCm) {
        this.mamcCm = mamcCm;
    }
}
