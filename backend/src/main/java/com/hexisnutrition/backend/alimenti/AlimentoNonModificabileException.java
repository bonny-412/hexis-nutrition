package com.hexisnutrition.backend.alimenti;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class AlimentoNonModificabileException extends RuntimeException {
}
