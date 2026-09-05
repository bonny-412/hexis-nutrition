package com.hexisnutrition.backend.alimenti;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

import java.util.UUID;

@RestController
@RequestMapping("/alimenti")
public class AlimentoController {

    private final AlimentoService alimentoService;

    public AlimentoController(AlimentoService alimentoService) {
        this.alimentoService = alimentoService;
    }

    @GetMapping("/ricerca")
    public AlimentoListaPaginataResponse ricerca(
            @AuthenticationPrincipal UUID professionistaId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int dimensione,
            @RequestParam(defaultValue = "nome") CampoOrdinamentoAlimenti ordinaPer,
            @RequestParam(defaultValue = "asc") DirezioneOrdinamento direzione,
            @RequestParam(required = false) String ricerca,
            @RequestParam(defaultValue = "TUTTI") FonteAlimento fonte) {
        int paginaEffettiva = Math.max(pagina, 0);
        int dimensioneEffettiva = Math.min(Math.max(dimensione, 1), 100);
        Sort.Direction direzioneSort = direzione == DirezioneOrdinamento.desc ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(paginaEffettiva, dimensioneEffettiva, Sort.by(direzioneSort, ordinaPer.name()));
        CriteriRicercaAlimenti criteri = new CriteriRicercaAlimenti(ricerca, fonte);
        Page<Alimento> risultato = alimentoService.cerca(professionistaId, criteri, pageable);
        return AlimentoListaPaginataResponse.da(risultato);
    }

    @GetMapping("/{id}")
    public AlimentoResponse dettaglio(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID id) {
        return AlimentoResponse.da(alimentoService.dettaglio(professionistaId, id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AlimentoResponse crea(@AuthenticationPrincipal UUID professionistaId,
                                  @Valid @RequestBody CreaAlimentoRequest request) {
        return AlimentoResponse.da(alimentoService.crea(professionistaId, request));
    }

    @PutMapping("/{id}")
    public AlimentoResponse aggiorna(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID id,
                                      @Valid @RequestBody AggiornaAlimentoRequest request) {
        return AlimentoResponse.da(alimentoService.aggiorna(professionistaId, id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void elimina(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID id) {
        alimentoService.elimina(professionistaId, id);
    }

    @PostMapping("/{id}/duplica")
    @ResponseStatus(HttpStatus.CREATED)
    public AlimentoResponse duplica(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID id) {
        return AlimentoResponse.da(alimentoService.duplica(professionistaId, id));
    }
}
