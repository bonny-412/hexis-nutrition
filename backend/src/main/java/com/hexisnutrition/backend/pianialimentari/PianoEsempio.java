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
@Table(name = "piano_esempi")
public class PianoEsempio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "piano_id", nullable = false)
    private UUID pianoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pasto", nullable = false)
    private TipoPasto tipoPasto;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private int ordine;

    @Column(columnDefinition = "TEXT")
    private String nota;

    protected PianoEsempio() {
    }

    public PianoEsempio(UUID pianoId, TipoPasto tipoPasto, String nome, int ordine, String nota) {
        this.pianoId = pianoId;
        this.tipoPasto = tipoPasto;
        this.nome = nome;
        this.ordine = ordine;
        this.nota = nota;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPianoId() {
        return pianoId;
    }

    public TipoPasto getTipoPasto() {
        return tipoPasto;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public int getOrdine() {
        return ordine;
    }

    public String getNota() {
        return nota;
    }
}
