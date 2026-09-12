package com.hexisnutrition.backend.pianialimentari;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "pasti")
public class Pasto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "piano_id", nullable = false)
    private UUID pianoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "giorno_settimana", nullable = false)
    private GiornoSettimana giornoSettimana;

    @Column(nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPasto tipo;

    @Column(columnDefinition = "TEXT")
    private String nota;

    @Column(nullable = false)
    private int ordine;

    protected Pasto() {
    }

    public Pasto(UUID pianoId, GiornoSettimana giornoSettimana, String nome, TipoPasto tipo, String nota, int ordine) {
        this.pianoId = pianoId;
        this.giornoSettimana = giornoSettimana;
        this.nome = nome;
        this.tipo = tipo;
        this.nota = nota;
        this.ordine = ordine;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPianoId() {
        return pianoId;
    }

    public GiornoSettimana getGiornoSettimana() {
        return giornoSettimana;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public TipoPasto getTipo() {
        return tipo;
    }

    public String getNota() {
        return nota;
    }

    public void setNota(String nota) {
        this.nota = nota;
    }

    public int getOrdine() {
        return ordine;
    }
}
