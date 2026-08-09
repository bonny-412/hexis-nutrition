package com.hexisnutrition.backend.inviti;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AttivaInvitoRequest(@NotBlank @Size(min = 8, max = 72) String nuovaPassword) {
}
