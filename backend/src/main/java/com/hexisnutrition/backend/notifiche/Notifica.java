package com.hexisnutrition.backend.notifiche;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Una notifica scattata per un appuntamento. Le righe si creano solo con {@link NotificaRepository#inserisciSeAssente}. */
@Entity
@Table(name = "notifiche")
public class Notifica {

    @Id
    private UUID id;

    @Column(name = "professionista_id", nullable = false)
    private UUID professionistaId;

    @Column(name = "appuntamento_id", nullable = false)
    private UUID appuntamentoId;

    @Column(name = "scatta_il", nullable = false)
    private Instant scattaIl;

    @Column(name = "creata_il", nullable = false)
    private Instant creataIl;

    @Column(name = "letta_il")
    private Instant lettaIl;

    @Column(name = "agenda_vista_il")
    private Instant agendaVistaIl;

    protected Notifica() {
    }

    public UUID getId() { return id; }
    public UUID getProfessionistaId() { return professionistaId; }
    public UUID getAppuntamentoId() { return appuntamentoId; }
    public Instant getScattaIl() { return scattaIl; }
    public Instant getCreataIl() { return creataIl; }

    public Instant getLettaIl() { return lettaIl; }
    public void setLettaIl(Instant lettaIl) { this.lettaIl = lettaIl; }

    public Instant getAgendaVistaIl() { return agendaVistaIl; }
    public void setAgendaVistaIl(Instant agendaVistaIl) { this.agendaVistaIl = agendaVistaIl; }
}
