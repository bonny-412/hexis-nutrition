package com.hexisnutrition.backend.inviti;

import com.hexisnutrition.backend.pazienti.PazienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inviti")
public class InvitoController {

    private final PazienteService pazienteService;

    public InvitoController(PazienteService pazienteService) {
        this.pazienteService = pazienteService;
    }

    @PostMapping("/{token}/attiva")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void attiva(@PathVariable String token, @Valid @RequestBody AttivaInvitoRequest request) {
        pazienteService.attiva(token, request.nuovaPassword());
    }
}
