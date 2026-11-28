package com.hexisnutrition.backend.idempotenza;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Le richieste con Idempotency-Key già ricevute, per utente. JDBC diretto: ogni operazione è una
 * transazione a sé, separata da quella della richiesta che protegge.
 */
@Component
public class RegistroIdempotenza {

    /** Risposta salvata; {@code statoHttp} null vuol dire che la prima richiesta è ancora in corso. */
    public record RichiestaRegistrata(String metodo, String percorso, Integer statoHttp, String contentType,
                                      String corpo, Instant creataIl) {
    }

    private final JdbcTemplate jdbc;

    public RegistroIdempotenza(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Segna la chiave come "in corso". Restituisce false se la chiave esiste già. */
    public boolean prenota(UUID utenteId, String chiave, String metodo, String percorso) {
        int inserite = jdbc.update("""
                INSERT INTO richieste_idempotenti (utente_id, chiave, metodo, percorso)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (utente_id, chiave) DO NOTHING
                """, utenteId, chiave, metodo, percorso);
        return inserite == 1;
    }

    public Optional<RichiestaRegistrata> trova(UUID utenteId, String chiave) {
        return jdbc.query("""
                SELECT metodo, percorso, stato_http, content_type, corpo, creata_il
                FROM richieste_idempotenti WHERE utente_id = ? AND chiave = ?
                """, (rs, i) -> new RichiestaRegistrata(
                rs.getString("metodo"),
                rs.getString("percorso"),
                (Integer) rs.getObject("stato_http"),
                rs.getString("content_type"),
                rs.getString("corpo"),
                rs.getTimestamp("creata_il").toInstant()), utenteId, chiave).stream().findFirst();
    }

    public void completa(UUID utenteId, String chiave, int statoHttp, String contentType, String corpo) {
        jdbc.update("""
                UPDATE richieste_idempotenti SET stato_http = ?, content_type = ?, corpo = ?
                WHERE utente_id = ? AND chiave = ?
                """, statoHttp, contentType, corpo, utenteId, chiave);
    }

    /** La richiesta non ha creato nulla (errore): la chiave si libera e un nuovo tentativo viene eseguito davvero. */
    public void rilascia(UUID utenteId, String chiave) {
        jdbc.update("DELETE FROM richieste_idempotenti WHERE utente_id = ? AND chiave = ?", utenteId, chiave);
    }

    public int cancellaPiuVecchieDi(Duration eta) {
        return jdbc.update("DELETE FROM richieste_idempotenti WHERE creata_il < ?",
                Timestamp.from(Instant.now().minus(eta)));
    }
}
