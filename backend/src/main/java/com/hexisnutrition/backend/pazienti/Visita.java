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

    @Column(name = "circonferenza_torace_cm")
    private BigDecimal circonferenzaToraceCm;

    @Column(name = "circonferenza_braccio_contratto_cm")
    private BigDecimal circonferenzaBraccioContrattoCm;

    @Enumerated(EnumType.STRING)
    @Column(name = "parte_dominante")
    private ParteDominante parteDominante;

    @Column(name = "circonferenza_braccio_rilassato_dominante_cm")
    private BigDecimal circonferenzaBraccioRilassatoDominanteCm;

    @Column(name = "circonferenza_braccio_contratto_dominante_cm")
    private BigDecimal circonferenzaBraccioContrattoDominanteCm;

    @Column(name = "circonferenza_coscia_dominante_cm")
    private BigDecimal circonferenzaCosciaDominanteCm;

    @Column(name = "circonferenza_polpaccio_dominante_cm")
    private BigDecimal circonferenzaPolpaccioDominanteCm;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ObiettivoVisita obiettivo = ObiettivoVisita.MANTENIMENTO;

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
                  BigDecimal circonferenzaToraceCm,
                  BigDecimal circonferenzaBraccioContrattoCm,
                  String note, ObiettivoVisita obiettivo) {
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
        this.circonferenzaToraceCm = circonferenzaToraceCm;
        this.circonferenzaBraccioContrattoCm = circonferenzaBraccioContrattoCm;
        this.note = note;
        if (obiettivo != null) {
            this.obiettivo = obiettivo;
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

    public void setDataVisita(LocalDate dataVisita) {
        this.dataVisita = dataVisita != null ? dataVisita : LocalDate.now();
    }

    public Integer getAltezzaCm() {
        return altezzaCm;
    }

    public void setAltezzaCm(Integer altezzaCm) {
        this.altezzaCm = altezzaCm;
    }

    public BigDecimal getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(BigDecimal pesoKg) {
        this.pesoKg = pesoKg;
    }

    public BigDecimal getCirconferenzaVitaCm() {
        return circonferenzaVitaCm;
    }

    public void setCirconferenzaVitaCm(BigDecimal circonferenzaVitaCm) {
        this.circonferenzaVitaCm = circonferenzaVitaCm;
    }

    public BigDecimal getCirconferenzaFianchiCm() {
        return circonferenzaFianchiCm;
    }

    public void setCirconferenzaFianchiCm(BigDecimal circonferenzaFianchiCm) {
        this.circonferenzaFianchiCm = circonferenzaFianchiCm;
    }

    public BigDecimal getCirconferenzaAddomeCm() {
        return circonferenzaAddomeCm;
    }

    public void setCirconferenzaAddomeCm(BigDecimal circonferenzaAddomeCm) {
        this.circonferenzaAddomeCm = circonferenzaAddomeCm;
    }

    public BigDecimal getCirconferenzaBraccioRilassatoCm() {
        return circonferenzaBraccioRilassatoCm;
    }

    public void setCirconferenzaBraccioRilassatoCm(BigDecimal circonferenzaBraccioRilassatoCm) {
        this.circonferenzaBraccioRilassatoCm = circonferenzaBraccioRilassatoCm;
    }

    public BigDecimal getCirconferenzaCosciaCm() {
        return circonferenzaCosciaCm;
    }

    public void setCirconferenzaCosciaCm(BigDecimal circonferenzaCosciaCm) {
        this.circonferenzaCosciaCm = circonferenzaCosciaCm;
    }

    public BigDecimal getCirconferenzaPolpaccioCm() {
        return circonferenzaPolpaccioCm;
    }

    public void setCirconferenzaPolpaccioCm(BigDecimal circonferenzaPolpaccioCm) {
        this.circonferenzaPolpaccioCm = circonferenzaPolpaccioCm;
    }

    public BigDecimal getCirconferenzaToraceCm() {
        return circonferenzaToraceCm;
    }

    public void setCirconferenzaToraceCm(BigDecimal circonferenzaToraceCm) {
        this.circonferenzaToraceCm = circonferenzaToraceCm;
    }

    public BigDecimal getCirconferenzaBraccioContrattoCm() {
        return circonferenzaBraccioContrattoCm;
    }

    public void setCirconferenzaBraccioContrattoCm(BigDecimal circonferenzaBraccioContrattoCm) {
        this.circonferenzaBraccioContrattoCm = circonferenzaBraccioContrattoCm;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public ObiettivoVisita getObiettivo() {
        return obiettivo;
    }

    public void setObiettivo(ObiettivoVisita obiettivo) {
        this.obiettivo = obiettivo != null ? obiettivo : ObiettivoVisita.MANTENIMENTO;
    }

    public BigDecimal getBmi() {
        return bmi;
    }

    public void setBmi(BigDecimal bmi) {
        this.bmi = bmi;
    }

    public ParteDominante getParteDominante() {
        return parteDominante;
    }

    public void setParteDominante(ParteDominante parteDominante) {
        this.parteDominante = parteDominante;
    }

    public BigDecimal getCirconferenzaBraccioRilassatoDominanteCm() {
        return circonferenzaBraccioRilassatoDominanteCm;
    }

    public void setCirconferenzaBraccioRilassatoDominanteCm(BigDecimal circonferenzaBraccioRilassatoDominanteCm) {
        this.circonferenzaBraccioRilassatoDominanteCm = circonferenzaBraccioRilassatoDominanteCm;
    }

    public BigDecimal getCirconferenzaBraccioContrattoDominanteCm() {
        return circonferenzaBraccioContrattoDominanteCm;
    }

    public void setCirconferenzaBraccioContrattoDominanteCm(BigDecimal circonferenzaBraccioContrattoDominanteCm) {
        this.circonferenzaBraccioContrattoDominanteCm = circonferenzaBraccioContrattoDominanteCm;
    }

    public BigDecimal getCirconferenzaCosciaDominanteCm() {
        return circonferenzaCosciaDominanteCm;
    }

    public void setCirconferenzaCosciaDominanteCm(BigDecimal circonferenzaCosciaDominanteCm) {
        this.circonferenzaCosciaDominanteCm = circonferenzaCosciaDominanteCm;
    }

    public BigDecimal getCirconferenzaPolpaccioDominanteCm() {
        return circonferenzaPolpaccioDominanteCm;
    }

    public void setCirconferenzaPolpaccioDominanteCm(BigDecimal circonferenzaPolpaccioDominanteCm) {
        this.circonferenzaPolpaccioDominanteCm = circonferenzaPolpaccioDominanteCm;
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
