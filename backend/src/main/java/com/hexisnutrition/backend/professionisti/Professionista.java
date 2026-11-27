package com.hexisnutrition.backend.professionisti;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "professionisti")
public class Professionista {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    /** Va nel claim "sv" del JWT; il reset della password la incrementa (V36, collaudo bug 19). */
    @Column(name = "versione_sessione", nullable = false)
    private int versioneSessione;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String cognome;

    @Column(name = "creato_il", nullable = false)
    private Instant creatoIl = Instant.now();

    protected Professionista() {
    }

    public Professionista(String email, String passwordHash, String nome, String cognome) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nome = nome;
        this.cognome = cognome;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /** Nuova password scelta dall'utente (reset): i token emessi prima smettono di valere. */
    public void cambiaPassword(String passwordHash) {
        this.passwordHash = passwordHash;
        this.versioneSessione++;
    }

    public int getVersioneSessione() {
        return versioneSessione;
    }

    public String getNome() {
        return nome;
    }

    public String getCognome() {
        return cognome;
    }
}
