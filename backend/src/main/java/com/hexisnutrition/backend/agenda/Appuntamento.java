package com.hexisnutrition.backend.agenda;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "appuntamenti")
public class Appuntamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "professionista_id", nullable = false)
    private UUID professionistaId;

    @Column(name = "paziente_id")
    private UUID pazienteId;

    @Column(name = "visita_id")
    private UUID visitaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAppuntamento tipo;

    @Enumerated(EnumType.STRING)
    private StatoAppuntamento stato;

    @Column(nullable = false)
    private LocalDate data;

    @Column(name = "ora_inizio")
    private LocalTime oraInizio;

    @Column(name = "durata_minuti")
    private Integer durataMinuti;

    private String testo;

    @Column(name = "link_video")
    private String linkVideo;

    @Column(nullable = false)
    private boolean fatto;

    @Column(nullable = false)
    private boolean notifica = true;

    @Column(name = "anticipo_notifica_minuti", nullable = false)
    private int anticipoNotificaMinuti = AgendaTempo.ANTICIPO_NOTIFICA_DEFAULT;

    @Column(name = "creato_il", nullable = false)
    private Instant creatoIl = Instant.now();

    protected Appuntamento() {
    }

    public Appuntamento(UUID professionistaId, TipoAppuntamento tipo, LocalDate data) {
        this.professionistaId = professionistaId;
        this.tipo = tipo;
        this.data = data;
    }

    public void concludi(UUID visitaId) {
        this.stato = StatoAppuntamento.CONCLUSA;
        this.visitaId = visitaId;
    }

    public void annulla() {
        this.stato = StatoAppuntamento.ANNULLATA;
    }

    public UUID getId() { return id; }
    public Instant getCreatoIl() { return creatoIl; }

    public UUID getProfessionistaId() { return professionistaId; }
    public void setProfessionistaId(UUID professionistaId) { this.professionistaId = professionistaId; }

    public UUID getPazienteId() { return pazienteId; }
    public void setPazienteId(UUID pazienteId) { this.pazienteId = pazienteId; }

    public UUID getVisitaId() { return visitaId; }
    public void setVisitaId(UUID visitaId) { this.visitaId = visitaId; }

    public TipoAppuntamento getTipo() { return tipo; }
    public void setTipo(TipoAppuntamento tipo) { this.tipo = tipo; }

    public StatoAppuntamento getStato() { return stato; }
    public void setStato(StatoAppuntamento stato) { this.stato = stato; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }

    public LocalTime getOraInizio() { return oraInizio; }
    public void setOraInizio(LocalTime oraInizio) { this.oraInizio = oraInizio; }

    public Integer getDurataMinuti() { return durataMinuti; }
    public void setDurataMinuti(Integer durataMinuti) { this.durataMinuti = durataMinuti; }

    public String getTesto() { return testo; }
    public void setTesto(String testo) { this.testo = testo; }

    public String getLinkVideo() { return linkVideo; }
    public void setLinkVideo(String linkVideo) { this.linkVideo = linkVideo; }

    public boolean isFatto() { return fatto; }
    public void setFatto(boolean fatto) { this.fatto = fatto; }

    public boolean isNotifica() { return notifica; }
    public void setNotifica(boolean notifica) { this.notifica = notifica; }

    public int getAnticipoNotificaMinuti() { return anticipoNotificaMinuti; }
    public void setAnticipoNotificaMinuti(int anticipoNotificaMinuti) { this.anticipoNotificaMinuti = anticipoNotificaMinuti; }
}
