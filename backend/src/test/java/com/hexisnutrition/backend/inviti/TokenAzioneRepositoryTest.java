package com.hexisnutrition.backend.inviti;

import com.hexisnutrition.backend.pazienti.Paziente;
import com.hexisnutrition.backend.pazienti.PazienteRepository;
import com.hexisnutrition.backend.professionisti.Professionista;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TokenAzioneRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private TokenAzioneRepository tokenAzioneRepository;

    @Autowired
    private PazienteRepository pazienteRepository;

    @Autowired
    private ProfessionistaRepository professionistaRepository;

    @AfterEach
    void pulisci() {
        tokenAzioneRepository.deleteAll();
        pazienteRepository.deleteAll();
        professionistaRepository.deleteAll();
    }

    @Test
    void salvaERitrovaPerToken() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "luca@example.com", null, null, null, null));

        TokenAzione token = TokenAzione.perPaziente(TipoToken.INVITO, paziente.getId(), Duration.ofDays(7));
        tokenAzioneRepository.save(token);

        Optional<TokenAzione> trovato = tokenAzioneRepository.findByToken(token.getToken());

        assertThat(trovato).isPresent();
        assertThat(trovato.get().getTipo()).isEqualTo(TipoToken.INVITO);
        assertThat(trovato.get().isValido()).isTrue();
    }

    @Test
    void unTokenScadutoNonEValido() {
        TokenAzione token = TokenAzione.perProfessionista(
                TipoToken.RESET_PASSWORD, UUID.randomUUID(), Duration.ofSeconds(-1));

        assertThat(token.isValido()).isFalse();
    }

    @Test
    void unTokenUsatoNonEValido() {
        TokenAzione token = TokenAzione.perProfessionista(
                TipoToken.RESET_PASSWORD, UUID.randomUUID(), Duration.ofHours(1));

        token.segnaUsato();

        assertThat(token.isValido()).isFalse();
    }
}
