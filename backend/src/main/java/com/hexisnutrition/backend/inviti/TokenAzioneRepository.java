package com.hexisnutrition.backend.inviti;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TokenAzioneRepository extends JpaRepository<TokenAzione, UUID> {
    Optional<TokenAzione> findByToken(String token);
}
