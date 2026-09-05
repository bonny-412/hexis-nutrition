package com.hexisnutrition.backend.alimenti;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "alimenti")
public class Alimento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "professionista_id")
    private UUID professionistaId;

    @Column(name = "codice_bda")
    private Integer codiceBda;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String categoria;

    @Column(name = "quantita_g", nullable = false)
    private BigDecimal quantitaG;

    @Column(nullable = false)
    private BigDecimal kcal;

    @Column(name = "proteine_g", nullable = false)
    private BigDecimal proteineG;

    @Column(name = "grassi_g", nullable = false)
    private BigDecimal grassiG;

    @Column(name = "carboidrati_g", nullable = false)
    private BigDecimal carboidratiG;

    @Column(name = "acqua_g")
    private BigDecimal acquaG;

    @Column(name = "fibre_g")
    private BigDecimal fibreG;

    @Column(name = "zuccheri_g")
    private BigDecimal zuccheriG;

    @Column(name = "ferro_mg")
    private BigDecimal ferroMg;

    @Column(name = "calcio_mg")
    private BigDecimal calcioMg;

    @Column(name = "sodio_mg")
    private BigDecimal sodioMg;

    protected Alimento() {
    }

    public Alimento(UUID professionistaId, String nome, String categoria, BigDecimal quantitaG, BigDecimal kcal,
                     BigDecimal proteineG, BigDecimal grassiG, BigDecimal carboidratiG, BigDecimal acquaG,
                     BigDecimal fibreG, BigDecimal zuccheriG, BigDecimal ferroMg, BigDecimal calcioMg,
                     BigDecimal sodioMg) {
        this.professionistaId = professionistaId;
        this.nome = nome;
        this.categoria = categoria;
        this.quantitaG = quantitaG;
        this.kcal = kcal;
        this.proteineG = proteineG;
        this.grassiG = grassiG;
        this.carboidratiG = carboidratiG;
        this.acquaG = acquaG;
        this.fibreG = fibreG;
        this.zuccheriG = zuccheriG;
        this.ferroMg = ferroMg;
        this.calcioMg = calcioMg;
        this.sodioMg = sodioMg;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProfessionistaId() {
        return professionistaId;
    }

    public Integer getCodiceBda() {
        return codiceBda;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public BigDecimal getQuantitaG() {
        return quantitaG;
    }

    public void setQuantitaG(BigDecimal quantitaG) {
        this.quantitaG = quantitaG;
    }

    public BigDecimal getKcal() {
        return kcal;
    }

    public void setKcal(BigDecimal kcal) {
        this.kcal = kcal;
    }

    public BigDecimal getProteineG() {
        return proteineG;
    }

    public void setProteineG(BigDecimal proteineG) {
        this.proteineG = proteineG;
    }

    public BigDecimal getGrassiG() {
        return grassiG;
    }

    public void setGrassiG(BigDecimal grassiG) {
        this.grassiG = grassiG;
    }

    public BigDecimal getCarboidratiG() {
        return carboidratiG;
    }

    public void setCarboidratiG(BigDecimal carboidratiG) {
        this.carboidratiG = carboidratiG;
    }

    public BigDecimal getAcquaG() {
        return acquaG;
    }

    public void setAcquaG(BigDecimal acquaG) {
        this.acquaG = acquaG;
    }

    public BigDecimal getFibreG() {
        return fibreG;
    }

    public void setFibreG(BigDecimal fibreG) {
        this.fibreG = fibreG;
    }

    public BigDecimal getZuccheriG() {
        return zuccheriG;
    }

    public void setZuccheriG(BigDecimal zuccheriG) {
        this.zuccheriG = zuccheriG;
    }

    public BigDecimal getFerroMg() {
        return ferroMg;
    }

    public void setFerroMg(BigDecimal ferroMg) {
        this.ferroMg = ferroMg;
    }

    public BigDecimal getCalcioMg() {
        return calcioMg;
    }

    public void setCalcioMg(BigDecimal calcioMg) {
        this.calcioMg = calcioMg;
    }

    public BigDecimal getSodioMg() {
        return sodioMg;
    }

    public void setSodioMg(BigDecimal sodioMg) {
        this.sodioMg = sodioMg;
    }

    public boolean isBda() {
        return professionistaId == null;
    }
}
