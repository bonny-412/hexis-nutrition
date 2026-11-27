package com.hexisnutrition.backend.agenda;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/agenda")
public class AgendaController {

    public record FattoRequest(@NotNull Boolean fatto) {
    }

    private final AgendaService agendaService;

    public AgendaController(AgendaService agendaService) {
        this.agendaService = agendaService;
    }

    @GetMapping
    public List<AppuntamentoResponse> elenco(
            @AuthenticationPrincipal UUID professionistaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate da,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate a) {
        return agendaService.elenco(professionistaId, da, a);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppuntamentoResponse crea(@AuthenticationPrincipal UUID professionistaId,
                                     @RequestParam(defaultValue = "false") boolean confermaSovrapposizione,
                                     @Valid @RequestBody AppuntamentoRequest request) {
        return agendaService.crea(professionistaId, request, confermaSovrapposizione);
    }

    @PutMapping("/{id}")
    public AppuntamentoResponse aggiorna(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID id,
                                         @RequestParam(defaultValue = "false") boolean confermaSovrapposizione,
                                         @Valid @RequestBody AppuntamentoRequest request) {
        return agendaService.aggiorna(professionistaId, id, request, confermaSovrapposizione);
    }

    @PostMapping("/{id}/annulla")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void annulla(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID id) {
        agendaService.annulla(professionistaId, id);
    }

    @PutMapping("/{id}/fatto")
    public AppuntamentoResponse fatto(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID id,
                                      @Valid @RequestBody FattoRequest request) {
        return agendaService.impostaFatto(professionistaId, id, request.fatto());
    }

    @ExceptionHandler(SovrapposizioneException.class)
    public ResponseEntity<Map<String, List<AppuntamentoResponse>>> sovrapposizione(SovrapposizioneException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("conflitti", e.getConflitti()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void elimina(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID id) {
        agendaService.elimina(professionistaId, id);
    }
}
