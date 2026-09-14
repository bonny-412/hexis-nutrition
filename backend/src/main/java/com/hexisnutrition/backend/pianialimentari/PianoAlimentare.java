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
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "piani_alimentari")
public class PianoAlimentare {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "paziente_id", nullable = false)
    private UUID pazienteId;

    @Column(name = "professionista_id", nullable = false)
    private UUID professionistaId;

    @Column(name = "visita_id")
    private UUID visitaId;

    @Column(nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModalitaPiano modalita;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatoPiano stato = StatoPiano.BOZZA;

    @Column(name = "data_inizio", nullable = false)
    private LocalDate dataInizio = LocalDate.now();

    @Column(name = "data_fine")
    private LocalDate dataFine;

    @Column(name = "obiettivo_kcal")
    private BigDecimal obiettivoKcal;

    @Column(name = "obiettivo_kcal_suggerito")
    private BigDecimal obiettivoKcalSuggerito;

    @Column(name = "bmr_calcolato")
    private BigDecimal bmrCalcolato;

    @Column(name = "tdee_calcolato")
    private BigDecimal tdeeCalcolato;

    @Enumerated(EnumType.STRING)
    @Column(name = "formula_bmr_usata")
    private FormulaBmr formulaBmrUsata;

    @Column(name = "sotto_soglia_sicurezza", nullable = false)
    private boolean sottoSogliaSicurezza;

    @Column(name = "creato_il", nullable = false)
    private Instant creatoIl = Instant.now();

    @Column(name = "aggiornato_il", nullable = false)
    private Instant aggiornatoIl = Instant.now();

    protected PianoAlimentare() {
    }

    public PianoAlimentare(UUID pazienteId, UUID professionistaId, UUID visitaId, String nome,
                            ModalitaPiano modalita) {
        this.pazienteId = pazienteId;
        this.professionistaId = professionistaId;
        this.visitaId = visitaId;
        this.nome = nome;
        this.modalita = modalita;
    }

    /** BOZZA/TERMINATO restano invariati; ATTIVO con dataFine passata diventa SCADUTO solo in lettura. */
    public StatoPianoVisualizzato statoEffettivo() {
        if (stato == StatoPiano.ATTIVO && dataFine != null && dataFine.isBefore(LocalDate.now())) {
            return StatoPianoVisualizzato.SCADUTO;
        }
        return StatoPianoVisualizzato.valueOf(stato.name());
    }

    public UUID getId() {
        return id;
    }

    public UUID getPazienteId() {
        return pazienteId;
    }

    public UUID getProfessionistaId() {
        return professionistaId;
    }

    public UUID getVisitaId() {
        return visitaId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public ModalitaPiano getModalita() {
        return modalita;
    }

    public StatoPiano getStato() {
        return stato;
    }

    public void setStato(StatoPiano stato) {
        this.stato = stato;
    }

    public LocalDate getDataInizio() {
        return dataInizio;
    }

    public void setDataInizio(LocalDate dataInizio) {
        this.dataInizio = dataInizio;
    }

    public LocalDate getDataFine() {
        return dataFine;
    }

    public void setDataFine(LocalDate dataFine) {
        this.dataFine = dataFine;
    }

    public BigDecimal getObiettivoKcal() {
        return obiettivoKcal;
    }

    public void setObiettivoKcal(BigDecimal obiettivoKcal) {
        this.obiettivoKcal = obiettivoKcal;
    }

    public BigDecimal getObiettivoKcalSuggerito() {
        return obiettivoKcalSuggerito;
    }

    public void setObiettivoKcalSuggerito(BigDecimal obiettivoKcalSuggerito) {
        this.obiettivoKcalSuggerito = obiettivoKcalSuggerito;
    }

    public BigDecimal getBmrCalcolato() {
        return bmrCalcolato;
    }

    public void setBmrCalcolato(BigDecimal bmrCalcolato) {
        this.bmrCalcolato = bmrCalcolato;
    }

    public BigDecimal getTdeeCalcolato() {
        return tdeeCalcolato;
    }

    public void setTdeeCalcolato(BigDecimal tdeeCalcolato) {
        this.tdeeCalcolato = tdeeCalcolato;
    }

    public FormulaBmr getFormulaBmrUsata() {
        return formulaBmrUsata;
    }

    public void setFormulaBmrUsata(FormulaBmr formulaBmrUsata) {
        this.formulaBmrUsata = formulaBmrUsata;
    }

    public boolean isSottoSogliaSicurezza() {
        return sottoSogliaSicurezza;
    }

    public void setSottoSogliaSicurezza(boolean sottoSogliaSicurezza) {
        this.sottoSogliaSicurezza = sottoSogliaSicurezza;
    }

    public Instant getCreatoIl() {
        return creatoIl;
    }
}
