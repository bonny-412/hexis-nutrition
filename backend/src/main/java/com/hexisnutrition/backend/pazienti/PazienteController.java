package com.hexisnutrition.backend.pazienti;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @GetMapping("/ricerca")
    public PazienteListaPaginataResponse ricerca(
            @AuthenticationPrincipal UUID professionistaId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int dimensione,
            @RequestParam(defaultValue = "nome") CampoOrdinamentoPazienti ordinaPer,
            @RequestParam(defaultValue = "asc") DirezioneOrdinamento direzione,
            @RequestParam(required = false) String ricerca,
            @RequestParam(required = false) StatoAccountPaziente statoAccount,
            @RequestParam(required = false) Sesso sesso,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataNascitaDa,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataNascitaA,
            @RequestParam(defaultValue = "false") boolean archiviato) {
        int paginaEffettiva = Math.max(pagina, 0);
        int dimensioneEffettiva = Math.min(Math.max(dimensione, 1), 100);
        Sort.Direction direzioneSort = direzione == DirezioneOrdinamento.desc ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(paginaEffettiva, dimensioneEffettiva, Sort.by(direzioneSort, ordinaPer.name()));
        CriteriRicercaPazienti criteri = new CriteriRicercaPazienti(ricerca, statoAccount, sesso, dataNascitaDa, dataNascitaA, archiviato);
        Page<Paziente> pagina1 = pazienteService.cerca(professionistaId, criteri, pageable);
        return PazienteListaPaginataResponse.da(pagina1);
    }

    @GetMapping("/{id}")
    public PazienteResponse dettaglio(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID id) {
        return PazienteResponse.da(pazienteService.dettaglio(professionistaId, id));
    }

    @PutMapping("/{id}")
    public PazienteResponse aggiorna(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID id,
                                      @Valid @RequestBody AggiornaPazienteRequest request) {
        return PazienteResponse.da(pazienteService.aggiorna(professionistaId, id, request));
    }

    @GetMapping("/{id}/visite")
    public List<VisitaResponse> visite(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID id) {
        return pazienteService.visite(professionistaId, id);
    }

    @PostMapping("/{id}/visite")
    @ResponseStatus(HttpStatus.CREATED)
    public VisitaResponse creaVisita(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID id,
                                      @Valid @RequestBody VisitaRequest request) {
        return pazienteService.creaVisita(professionistaId, id, request);
    }

    @GetMapping("/{id}/visite/{visitaId}")
    public VisitaResponse dettaglioVisita(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID id,
                                           @PathVariable UUID visitaId) {
        return pazienteService.visitaSingola(professionistaId, id, visitaId);
    }

    @PutMapping("/{id}/visite/{visitaId}")
    public VisitaResponse aggiornaVisita(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID id,
                                          @PathVariable UUID visitaId, @Valid @RequestBody VisitaRequest request) {
        return pazienteService.aggiornaVisita(professionistaId, id, visitaId, request);
    }

    @PostMapping("/{id}/invito")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void invita(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID id) {
        pazienteService.invita(professionistaId, id);
    }

    @PostMapping("/{id}/archivia")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archivia(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID id) {
        pazienteService.archivia(professionistaId, id);
    }

    @PostMapping("/{id}/de-archivia")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deArchivia(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID id) {
        pazienteService.deArchivia(professionistaId, id);
    }
}
