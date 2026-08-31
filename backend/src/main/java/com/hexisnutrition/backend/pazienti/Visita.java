package com.hexisnutrition.backend.pazienti;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(name = "circonferenza_ombelico_cm")
    private BigDecimal circonferenzaOmbelicoCm;

    @Column(name = "circonferenza_fianchi_cm")
    private BigDecimal circonferenzaFianchiCm;

    @Column(name = "circonferenza_petto_cm")
    private BigDecimal circonferenzaPettoCm;

    @Column(name = "circonferenza_coscia_dx_cm")
    private BigDecimal circonferenzaCosciaDxCm;

    @Column(name = "circonferenza_coscia_sx_cm")
    private BigDecimal circonferenzaCosciaSxCm;

    @Column(name = "circonferenza_polpaccio_dx_cm")
    private BigDecimal circonferenzaPolpaccioDxCm;

    @Column(name = "circonferenza_polpaccio_sx_cm")
    private BigDecimal circonferenzaPolpaccioSxCm;

    @Column(name = "larghezza_spalle_cm")
    private BigDecimal larghezzaSpalleCm;

    @Column(name = "circonferenza_spalle_cm")
    private BigDecimal circonferenzaSpalleCm;

    @Column(name = "circonferenza_bicipite_dx_cm")
    private BigDecimal circonferenzaBicipiteDxCm;

    @Column(name = "circonferenza_bicipite_sx_cm")
    private BigDecimal circonferenzaBicipiteSxCm;

    @Column(name = "creato_il", nullable = false)
    private Instant creatoIl = Instant.now();

    protected Visita() {
    }

    public Visita(UUID pazienteId, LocalDate dataVisita, Integer altezzaCm, BigDecimal pesoKg,
                  BigDecimal circonferenzaVitaCm, BigDecimal circonferenzaOmbelicoCm,
                  BigDecimal circonferenzaFianchiCm, BigDecimal circonferenzaPettoCm,
                  BigDecimal circonferenzaCosciaDxCm, BigDecimal circonferenzaCosciaSxCm,
                  BigDecimal circonferenzaPolpaccioDxCm, BigDecimal circonferenzaPolpaccioSxCm,
                  BigDecimal larghezzaSpalleCm, BigDecimal circonferenzaSpalleCm,
                  BigDecimal circonferenzaBicipiteDxCm, BigDecimal circonferenzaBicipiteSxCm) {
        this.pazienteId = pazienteId;
        if (dataVisita != null) {
            this.dataVisita = dataVisita;
        }
        this.altezzaCm = altezzaCm;
        this.pesoKg = pesoKg;
        this.circonferenzaVitaCm = circonferenzaVitaCm;
        this.circonferenzaOmbelicoCm = circonferenzaOmbelicoCm;
        this.circonferenzaFianchiCm = circonferenzaFianchiCm;
        this.circonferenzaPettoCm = circonferenzaPettoCm;
        this.circonferenzaCosciaDxCm = circonferenzaCosciaDxCm;
        this.circonferenzaCosciaSxCm = circonferenzaCosciaSxCm;
        this.circonferenzaPolpaccioDxCm = circonferenzaPolpaccioDxCm;
        this.circonferenzaPolpaccioSxCm = circonferenzaPolpaccioSxCm;
        this.larghezzaSpalleCm = larghezzaSpalleCm;
        this.circonferenzaSpalleCm = circonferenzaSpalleCm;
        this.circonferenzaBicipiteDxCm = circonferenzaBicipiteDxCm;
        this.circonferenzaBicipiteSxCm = circonferenzaBicipiteSxCm;
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

    public BigDecimal getCirconferenzaOmbelicoCm() {
        return circonferenzaOmbelicoCm;
    }

    public BigDecimal getCirconferenzaFianchiCm() {
        return circonferenzaFianchiCm;
    }

    public BigDecimal getCirconferenzaPettoCm() {
        return circonferenzaPettoCm;
    }

    public BigDecimal getCirconferenzaCosciaDxCm() {
        return circonferenzaCosciaDxCm;
    }

    public BigDecimal getCirconferenzaCosciaSxCm() {
        return circonferenzaCosciaSxCm;
    }

    public BigDecimal getCirconferenzaPolpaccioDxCm() {
        return circonferenzaPolpaccioDxCm;
    }

    public BigDecimal getCirconferenzaPolpaccioSxCm() {
        return circonferenzaPolpaccioSxCm;
    }

    public BigDecimal getLarghezzaSpalleCm() {
        return larghezzaSpalleCm;
    }

    public BigDecimal getCirconferenzaSpalleCm() {
        return circonferenzaSpalleCm;
    }

    public BigDecimal getCirconferenzaBicipiteDxCm() {
        return circonferenzaBicipiteDxCm;
    }

    public BigDecimal getCirconferenzaBicipiteSxCm() {
        return circonferenzaBicipiteSxCm;
    }
}
