package com.hexisnutrition.backend.inviti;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class TokenAzionePulizia {

    private final TokenAzioneRepository tokenAzioneRepository;

    public TokenAzionePulizia(TokenAzioneRepository tokenAzioneRepository) {
        this.tokenAzioneRepository = tokenAzioneRepository;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void pulisciScaduti() {
        tokenAzioneRepository.deleteByScadenzaBefore(Instant.now());
    }
}
