package com.hexisnutrition.backend.pianialimentari;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "piano_note")
public class PianoNota {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "piano_id", nullable = false)
    private UUID pianoId;

    @Column(name = "autore_id", nullable = false)
    private UUID autoreId;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String testo;

    @Column(name = "creato_il", nullable = false)
    private Instant creatoIl = Instant.now();

    protected PianoNota() {
    }

    public PianoNota(UUID pianoId, UUID autoreId, LocalDate data, String testo) {
        this.pianoId = pianoId;
        this.autoreId = autoreId;
        this.data = data;
        this.testo = testo;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPianoId() {
        return pianoId;
    }

    public UUID getAutoreId() {
        return autoreId;
    }

    public LocalDate getData() {
        return data;
    }

    public String getTesto() {
        return testo;
    }

    public Instant getCreatoIl() {
        return creatoIl;
    }
}
