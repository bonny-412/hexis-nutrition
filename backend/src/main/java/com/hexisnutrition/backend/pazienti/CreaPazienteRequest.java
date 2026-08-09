package com.hexisnutrition.backend.pazienti;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record CreaPazienteRequest(
        @NotBlank String nome,
        @NotBlank String cognome,
        @NotBlank @Email String email,
        String telefono,
        LocalDate dataNascita,
        String sesso,
        Integer altezzaCm
) {
}
