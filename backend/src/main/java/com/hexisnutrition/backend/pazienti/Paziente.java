package com.hexisnutrition.backend.pazienti;

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
import java.util.UUID;

@Entity
@Table(name = "pazienti")
public class Paziente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "professionista_id", nullable = false)
    private UUID professionistaId;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String cognome;

    @Column(name = "codice_fiscale", nullable = false)
    private String codiceFiscale;

    @Column(nullable = false)
    private String email;

    private String telefono;

    @Column(name = "data_nascita", nullable = false)
    private LocalDate dataNascita;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sesso sesso;

    private String lavoro;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_lavoro")
    private TipoLavoro tipoLavoro;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "stato_account", nullable = false)
    private StatoAccountPaziente statoAccount = StatoAccountPaziente.MAI_INVITATO;

    @Column(nullable = false)
    private boolean archiviato = false;

    @Column(name = "creato_il", nullable = false)
    private Instant creatoIl = Instant.now();

    protected Paziente() {
    }

    public Paziente(UUID professionistaId, String nome, String cognome, String codiceFiscale, String email,
                     String telefono, LocalDate dataNascita, Sesso sesso, String lavoro, TipoLavoro tipoLavoro,
                     String note) {
        this.professionistaId = professionistaId;
        this.nome = nome;
        this.cognome = cognome;
        this.codiceFiscale = codiceFiscale;
        this.email = email;
        this.telefono = telefono;
        this.dataNascita = dataNascita;
        this.sesso = sesso;
        this.lavoro = lavoro;
        this.tipoLavoro = tipoLavoro;
        this.note = note;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProfessionistaId() {
        return professionistaId;
    }

    public String getNome() {
        return nome;
    }

    public String getCognome() {
        return cognome;
    }

    public String getCodiceFiscale() {
        return codiceFiscale;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefono() {
        return telefono;
    }

    public LocalDate getDataNascita() {
        return dataNascita;
    }

    public Sesso getSesso() {
        return sesso;
    }

    public String getLavoro() {
        return lavoro;
    }

    public TipoLavoro getTipoLavoro() {
        return tipoLavoro;
    }

    public String getNote() {
        return note;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public StatoAccountPaziente getStatoAccount() {
        return statoAccount;
    }

    public void setStatoAccount(StatoAccountPaziente statoAccount) {
        this.statoAccount = statoAccount;
    }

    public boolean isArchiviato() {
        return archiviato;
    }

    public void setArchiviato(boolean archiviato) {
        this.archiviato = archiviato;
    }
}
