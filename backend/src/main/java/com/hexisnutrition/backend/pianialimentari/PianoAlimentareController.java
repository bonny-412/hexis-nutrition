package com.hexisnutrition.backend.pianialimentari;

import com.hexisnutrition.backend.pazienti.PazienteRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/piani-alimentari")
public class PianoAlimentareController {

    private final PianoAlimentareService pianoAlimentareService;
    private final PazienteRepository pazienteRepository;

    public PianoAlimentareController(PianoAlimentareService pianoAlimentareService,
            PazienteRepository pazienteRepository) {
        this.pianoAlimentareService = pianoAlimentareService;
        this.pazienteRepository = pazienteRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PianoAlimentareResponse crea(@AuthenticationPrincipal UUID professionistaId,
                                          @Valid @RequestBody CreaPianoAlimentareRequest request) {
        return pianoAlimentareService.creaBozza(professionistaId, request);
    }

    @GetMapping("/ricerca")
    public PianoAlimentareListaPaginataResponse ricerca(
            @AuthenticationPrincipal UUID professionistaId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int dimensione,
            @RequestParam(defaultValue = "nome") CampoOrdinamentoPianiAlimentari ordinaPer,
            @RequestParam(defaultValue = "asc") DirezioneOrdinamento direzione,
            @RequestParam(required = false) String ricerca,
            @RequestParam(required = false) StatoPianoVisualizzato stato,
            @RequestParam(required = false) UUID pazienteId) {
        int paginaEffettiva = Math.max(pagina, 0);
        int dimensioneEffettiva = Math.min(Math.max(dimensione, 1), 100);
        org.springframework.data.domain.Sort.Direction direzioneSort =
                direzione == DirezioneOrdinamento.desc
                        ? org.springframework.data.domain.Sort.Direction.DESC
                        : org.springframework.data.domain.Sort.Direction.ASC;
        var pageable = org.springframework.data.domain.PageRequest.of(paginaEffettiva, dimensioneEffettiva,
                org.springframework.data.domain.Sort.by(direzioneSort, ordinaPer.name()));
        var paginaRisultati = pianoAlimentareService.cerca(professionistaId,
                new CriteriRicercaPianiAlimentari(ricerca, stato, pazienteId), pageable);

        var pazientiPerId = pazienteRepository.findAllById(
                paginaRisultati.getContent().stream().map(PianoAlimentare::getPazienteId).distinct().toList()).stream()
                .collect(java.util.stream.Collectors.toMap(com.hexisnutrition.backend.pazienti.Paziente::getId,
                        p -> p.getNome() + " " + p.getCognome()));

        var contenuto = paginaRisultati.getContent().stream()
                .map(p -> new PianoAlimentareRigaListaResponse(p.getId(),
                        pazientiPerId.getOrDefault(p.getPazienteId(), "—"), p.getNome(), p.statoEffettivo(),
                        p.getObiettivoKcal(), p.getDataFine()))
                .toList();

        return new PianoAlimentareListaPaginataResponse(contenuto, paginaRisultati.getNumber(),
                paginaRisultati.getSize(), paginaRisultati.getTotalElements(), paginaRisultati.getTotalPages());
    }

    @GetMapping("/{id}")
    public PianoAlimentareResponse dettaglio(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID id) {
        return pianoAlimentareService.dettaglio(professionistaId, id);
    }

    @PutMapping("/{id}")
    public PianoAlimentareResponse aggiorna(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID id,
                                              @Valid @RequestBody AggiornaPianoAlimentareRequest request) {
        return pianoAlimentareService.aggiorna(professionistaId, id, request);
    }

    @PostMapping("/{id}/attiva")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void attiva(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID id) {
        pianoAlimentareService.attiva(professionistaId, id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void elimina(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID id) {
        pianoAlimentareService.elimina(professionistaId, id);
    }
}
