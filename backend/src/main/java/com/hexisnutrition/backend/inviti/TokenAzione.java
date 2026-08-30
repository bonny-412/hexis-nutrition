package com.hexisnutrition.backend.inviti;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Entity
@Table(name = "token_azione")
public class TokenAzione {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    /**
     * Il valore in chiaro non è mai persistito: esiste solo sull'istanza appena
     * creata, il tempo di comporre l'email. Dopo un ricaricamento dal database è null.
     */
    @Transient
    private String tokenChiaro;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoToken tipo;

    @Column(name = "professionista_id")
    private UUID professionistaId;

    @Column(name = "paziente_id")
    private UUID pazienteId;

    @Column(nullable = false)
    private Instant scadenza;

    @Column(name = "creato_il", nullable = false)
    private Instant creatoIl = Instant.now();

    protected TokenAzione() {
    }

    public static TokenAzione perPaziente(TipoToken tipo, UUID pazienteId, Duration validita) {
        TokenAzione token = new TokenAzione();
        token.impostaTokenChiaro(UUID.randomUUID().toString());
        token.tipo = tipo;
        token.pazienteId = pazienteId;
        token.scadenza = Instant.now().plus(validita);
        return token;
    }

    public static TokenAzione perProfessionista(TipoToken tipo, UUID professionistaId, Duration validita) {
        TokenAzione token = new TokenAzione();
        token.impostaTokenChiaro(UUID.randomUUID().toString());
        token.tipo = tipo;
        token.professionistaId = professionistaId;
        token.scadenza = Instant.now().plus(validita);
        return token;
    }

    private void impostaTokenChiaro(String valore) {
        this.tokenChiaro = valore;
        this.tokenHash = hash(valore);
    }

    public static String hash(String tokenChiaro) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(tokenChiaro.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean isValido() {
        return Instant.now().isBefore(scadenza);
    }

    public UUID getId() {
        return id;
    }

    /**
     * Restituisce il token in chiaro: valorizzato solo sull'istanza appena creata
     * con {@link #perPaziente} / {@link #perProfessionista}, per comporre l'email.
     */
    public String getToken() {
        return tokenChiaro;
    }

    public String getTokenHash() {
        return tokenHash;
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
}
