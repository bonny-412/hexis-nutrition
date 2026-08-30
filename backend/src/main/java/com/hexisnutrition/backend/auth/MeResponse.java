package com.hexisnutrition.backend.auth;

import java.util.UUID;

public record MeResponse(UUID id, String nome, String cognome, String email, String ruolo) {
}
