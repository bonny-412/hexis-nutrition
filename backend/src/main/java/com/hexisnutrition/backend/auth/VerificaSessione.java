package com.hexisnutrition.backend.auth;

import com.hexisnutrition.backend.pazienti.PazienteRepository;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Un JWT vale solo se la sua versione di sessione coincide con quella attuale dell'utente:
 * il reset della password la incrementa e chiude così le sessioni aperte prima (collaudo, bug 19).
 * Una lettura per richiesta, sulla chiave primaria.
 */
@Component
public class VerificaSessione {

    private final ProfessionistaRepository professionistaRepository;
    private final PazienteRepository pazienteRepository;

    public VerificaSessione(ProfessionistaRepository professionistaRepository, PazienteRepository pazienteRepository) {
        this.professionistaRepository = professionistaRepository;
        this.pazienteRepository = pazienteRepository;
    }

    public boolean valida(UUID userId, Ruolo ruolo, int versioneNelToken) {
        Optional<Integer> attuale = ruolo == Ruolo.PROFESSIONISTA
                ? professionistaRepository.versioneSessione(userId)
                : pazienteRepository.versioneSessione(userId);
        return attuale.map(v -> v == versioneNelToken).orElse(false);
    }
}
