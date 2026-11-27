package com.hexisnutrition.backend.pazienti;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreaPazienteRequest(
        @NotBlank @Size(max = 100) String nome,
        @NotBlank @Size(max = 100) String cognome,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9]{16}$", message = "Il codice fiscale deve avere 16 caratteri alfanumerici.") String codiceFiscale,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 30) String telefono,
        @Size(max = 30) String cellulare,
        @NotNull LocalDate dataNascita,
        @NotNull Sesso sesso,
        @Size(max = 150) String lavoro,
        @Size(max = 255) String medicoRiferimento,
        StileDiVita stileDiVita,
        @Size(max = 5000) String note,
        @Size(max = 5000) String trattamenti,
        @NotNull @Valid VisitaRequest visita
) {
}
