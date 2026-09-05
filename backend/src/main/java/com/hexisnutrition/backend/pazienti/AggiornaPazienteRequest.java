package com.hexisnutrition.backend.pazienti;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record AggiornaPazienteRequest(
        @NotBlank String nome,
        @NotBlank String cognome,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9]{16}$", message = "Il codice fiscale deve avere 16 caratteri alfanumerici.") String codiceFiscale,
        @NotBlank @Email String email,
        String telefono,
        @NotNull LocalDate dataNascita,
        @NotNull Sesso sesso,
        String lavoro,
        StileDiVita stileDiVita,
        String note
) {
}
