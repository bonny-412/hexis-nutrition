package com.hexisnutrition.backend.pazienti;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pazienti")
public class PazienteController {

    private final PazienteService pazienteService;

    public PazienteController(PazienteService pazienteService) {
        this.pazienteService = pazienteService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PazienteResponse crea(@AuthenticationPrincipal UUID professionistaId,
                                  @Valid @RequestBody CreaPazienteRequest request) {
        return PazienteResponse.da(pazienteService.crea(professionistaId, request));
    }

    @GetMapping
    public List<PazienteResponse> lista(@AuthenticationPrincipal UUID professionistaId) {
        return pazienteService.listaPerProfessionista(professionistaId).stream()
                .map(PazienteResponse::da)
                .toList();
    }

    @GetMapping("/{id}")
    public PazienteResponse dettaglio(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID id) {
        return PazienteResponse.da(pazienteService.dettaglio(professionistaId, id));
    }

    @PostMapping("/{id}/invito")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void invita(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID id) {
        pazienteService.invita(professionistaId, id);
    }
}
