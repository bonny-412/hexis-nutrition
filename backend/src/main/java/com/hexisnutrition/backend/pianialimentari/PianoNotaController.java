package com.hexisnutrition.backend.pianialimentari;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/piani-alimentari/{pianoId}/note")
public class PianoNotaController {

    private final PianoNotaService pianoNotaService;

    public PianoNotaController(PianoNotaService pianoNotaService) {
        this.pianoNotaService = pianoNotaService;
    }

    @GetMapping
    public List<NotaPianoResponse> elenco(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID pianoId) {
        return pianoNotaService.elenco(professionistaId, pianoId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NotaPianoResponse aggiungi(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID pianoId,
            @Valid @RequestBody CreaNotaPianoRequest request) {
        return pianoNotaService.aggiungi(professionistaId, pianoId, request);
    }

    @DeleteMapping("/{notaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void elimina(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID pianoId,
            @PathVariable UUID notaId) {
        pianoNotaService.elimina(professionistaId, pianoId, notaId);
    }
}
