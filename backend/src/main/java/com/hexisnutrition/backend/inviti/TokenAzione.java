package com.hexisnutrition.backend.inviti;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "token_azione")
public class TokenAzione {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoToken tipo;

    @Column(name = "professionista_id")
    private UUID professionistaId;

    @Column(name = "paziente_id")
    private UUID pazienteId;

    @Column(nullable = false)
    private Instant scadenza;

    @Column(nullable = false)
    private boolean usato = false;

    @Column(name = "creato_il", nullable = false)
    private Instant creatoIl = Instant.now();

    protected TokenAzione() {
    }

    public static TokenAzione perPaziente(TipoToken tipo, UUID pazienteId, Duration validita) {
        TokenAzione token = new TokenAzione();
        token.token = UUID.randomUUID().toString();
        token.tipo = tipo;
        token.pazienteId = pazienteId;
        token.scadenza = Instant.now().plus(validita);
        return token;
    }

    public static TokenAzione perProfessionista(TipoToken tipo, UUID professionistaId, Duration validita) {
        TokenAzione token = new TokenAzione();
        token.token = UUID.randomUUID().toString();
        token.tipo = tipo;
        token.professionistaId = professionistaId;
        token.scadenza = Instant.now().plus(validita);
        return token;
    }

    public boolean isValido() {
        return !usato && Instant.now().isBefore(scadenza);
    }

    public void segnaUsato() {
        this.usato = true;
    }

    public UUID getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public TipoToken getTipo() {
        return tipo;
    }

    public UUID getProfessionistaId() {
        return professionistaId;
    }

    public UUID getPazienteId() {
        return pazienteId;
    }

    public Instant getScadenza() {
        return scadenza;
    }

    public boolean isUsato() {
        return usato;
    }
}
