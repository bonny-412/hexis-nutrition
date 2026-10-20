package com.hexisnutrition.backend.pianialimentari;

import com.hexisnutrition.backend.professionisti.Professionista;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PianoNotaService {

    private final PianoAlimentareRepository pianoAlimentareRepository;
    private final PianoNotaRepository pianoNotaRepository;
    private final ProfessionistaRepository professionistaRepository;

    public PianoNotaService(PianoAlimentareRepository pianoAlimentareRepository,
            PianoNotaRepository pianoNotaRepository, ProfessionistaRepository professionistaRepository) {
        this.pianoAlimentareRepository = pianoAlimentareRepository;
        this.pianoNotaRepository = pianoNotaRepository;
        this.professionistaRepository = professionistaRepository;
    }

    /** Note del piano, dalla data più recente; sempre consultabili (per una bozza l'elenco è vuoto). */
    @Transactional(readOnly = true)
    public List<NotaPianoResponse> elenco(UUID professionistaId, UUID pianoId) {
        pianoDiProprieta(professionistaId, pianoId);
        List<PianoNota> note = pianoNotaRepository.findAllByPianoIdOrderByDataDescCreatoIlDesc(pianoId);
        Map<UUID, String> autori = professionistaRepository
                .findAllById(note.stream().map(PianoNota::getAutoreId).distinct().toList()).stream()
                .collect(Collectors.toMap(Professionista::getId, p -> p.getNome() + " " + p.getCognome()));
        return note.stream().map(n -> NotaPianoResponse.da(n, autori.getOrDefault(n.getAutoreId(), "—"))).toList();
    }

    @Transactional
    public NotaPianoResponse aggiungi(UUID professionistaId, UUID pianoId, CreaNotaPianoRequest richiesta) {
        pianoModificabileConNote(professionistaId, pianoId);
        PianoNota nota = pianoNotaRepository.save(
                new PianoNota(pianoId, professionistaId, richiesta.data(), richiesta.testo().trim()));
        Professionista autore = professionistaRepository.findById(professionistaId).orElseThrow();
        return NotaPianoResponse.da(nota, autore.getNome() + " " + autore.getCognome());
    }

    @Transactional
    public void elimina(UUID professionistaId, UUID pianoId, UUID notaId) {
        pianoModificabileConNote(professionistaId, pianoId);
        PianoNota nota = pianoNotaRepository.findByIdAndPianoId(notaId, pianoId)
                .orElseThrow(NotaPianoNonTrovataException::new);
        pianoNotaRepository.delete(nota);
    }

    private PianoAlimentare pianoDiProprieta(UUID professionistaId, UUID pianoId) {
        PianoAlimentare piano = pianoAlimentareRepository.findById(pianoId)
                .orElseThrow(PianoAlimentareNonTrovatoException::new);
        if (!piano.getProfessionistaId().equals(professionistaId)) {
            throw new PianoAlimentareNonTrovatoException();
        }
        return piano;
    }

    /** Solo un piano attivo accetta modifiche alle note: la scadenza (data di fine passata) lo rende solo consultabile. */
    private void pianoModificabileConNote(UUID professionistaId, UUID pianoId) {
        PianoAlimentare piano = pianoDiProprieta(professionistaId, pianoId);
        if (piano.statoEffettivo() != StatoPianoVisualizzato.ATTIVO) {
            throw new PianoNoteNonModificabiliException();
        }
    }
}
