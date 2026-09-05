package com.hexisnutrition.backend.alimenti;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class AlimentoNonTrovatoException extends RuntimeException {
}
