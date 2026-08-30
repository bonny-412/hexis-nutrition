package com.hexisnutrition.backend.inviti;

import com.hexisnutrition.backend.professionisti.Professionista;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class TokenAzionePuliziaTest extends AbstractIntegrationTest {

    @Autowired
    private TokenAzioneRepository tokenAzioneRepository;

    @Autowired
    private ProfessionistaRepository professionistaRepository;

    @Autowired
    private TokenAzionePulizia tokenAzionePulizia;

    @AfterEach
    void pulisci() {
        tokenAzioneRepository.deleteAll();
        professionistaRepository.deleteAll();
    }

    @Test
    void cancellaITokenScadutiELascialiValidi() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("pulizia@example.com", "hash", "Anna", "Bianchi"));

        TokenAzione scaduto = TokenAzione.perProfessionista(
                TipoToken.RESET_PASSWORD, professionista.getId(), Duration.ofSeconds(-1));
        TokenAzione valido = TokenAzione.perProfessionista(
                TipoToken.RESET_PASSWORD, professionista.getId(), Duration.ofHours(1));
        tokenAzioneRepository.save(scaduto);
        tokenAzioneRepository.save(valido);

        tokenAzionePulizia.pulisciScaduti();

        assertThat(tokenAzioneRepository.findByTokenHash(TokenAzione.hash(scaduto.getToken()))).isEmpty();
        assertThat(tokenAzioneRepository.findByTokenHash(TokenAzione.hash(valido.getToken()))).isPresent();
    }
}
