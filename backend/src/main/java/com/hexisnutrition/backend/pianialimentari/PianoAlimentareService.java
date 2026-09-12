package com.hexisnutrition.backend.pianialimentari;

import com.hexisnutrition.backend.pazienti.Paziente;
import com.hexisnutrition.backend.pazienti.PazienteNonTrovatoException;
import com.hexisnutrition.backend.pazienti.PazienteRepository;
import com.hexisnutrition.backend.pazienti.Plicometria;
import com.hexisnutrition.backend.pazienti.PlicometriaRepository;
import com.hexisnutrition.backend.pazienti.Visita;
import com.hexisnutrition.backend.pazienti.VisitaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PianoAlimentareService {

    private static final List<TipoPasto> PASTI_TEMPLATE = List.of(
            TipoPasto.COLAZIONE, TipoPasto.SPUNTINO_MATTINA, TipoPasto.PRANZO,
            TipoPasto.SPUNTINO_POMERIGGIO, TipoPasto.CENA);
    private static final Map<TipoPasto, String> NOMI_PASTI_TEMPLATE = Map.of(
            TipoPasto.COLAZIONE, "Colazione",
            TipoPasto.SPUNTINO_MATTINA, "Spuntino mattina",
            TipoPasto.PRANZO, "Pranzo",
            TipoPasto.SPUNTINO_POMERIGGIO, "Spuntino pomeriggio",
            TipoPasto.CENA, "Cena");

    private final PianoAlimentareRepository pianoAlimentareRepository;
    private final PastoRepository pastoRepository;
    private final PianoGiornoMacroTargetRepository pianoGiornoMacroTargetRepository;
    private final PianoEsempioRepository pianoEsempioRepository;
    private final PianoAlimentoRigaRepository pianoAlimentoRigaRepository;
    private final PazienteRepository pazienteRepository;
    private final VisitaRepository visitaRepository;
    private final PlicometriaRepository plicometriaRepository;
    private final CalcolatoreTdee calcolatoreTdee;

    public PianoAlimentareService(PianoAlimentareRepository pianoAlimentareRepository, PastoRepository pastoRepository,
            PianoGiornoMacroTargetRepository pianoGiornoMacroTargetRepository,
            PianoEsempioRepository pianoEsempioRepository, PianoAlimentoRigaRepository pianoAlimentoRigaRepository,
            PazienteRepository pazienteRepository, VisitaRepository visitaRepository,
            PlicometriaRepository plicometriaRepository, CalcolatoreTdee calcolatoreTdee) {
        this.pianoAlimentareRepository = pianoAlimentareRepository;
        this.pastoRepository = pastoRepository;
        this.pianoGiornoMacroTargetRepository = pianoGiornoMacroTargetRepository;
        this.pianoEsempioRepository = pianoEsempioRepository;
        this.pianoAlimentoRigaRepository = pianoAlimentoRigaRepository;
        this.pazienteRepository = pazienteRepository;
        this.visitaRepository = visitaRepository;
        this.plicometriaRepository = plicometriaRepository;
        this.calcolatoreTdee = calcolatoreTdee;
    }

    private Paziente pazienteDiProprieta(UUID professionistaId, UUID pazienteId) {
        Paziente paziente = pazienteRepository.findById(pazienteId).orElseThrow(PazienteNonTrovatoException::new);
        if (!paziente.getProfessionistaId().equals(professionistaId)) {
            throw new PazienteNonTrovatoException();
        }
        return paziente;
    }

    private Optional<Visita> ultimaVisita(UUID pazienteId) {
        return visitaRepository.findAllByPazienteIdOrderByDataVisitaAsc(pazienteId).stream()
                .max(Comparator.comparing(Visita::getDataVisita));
    }

    @Transactional
    public PianoAlimentareResponse creaBozza(UUID professionistaId, CreaPianoAlimentareRequest request) {
        Paziente paziente = pazienteDiProprieta(professionistaId, request.pazienteId());

        Optional<Visita> visitaOpt = ultimaVisita(paziente.getId());
        PianoAlimentare piano = new PianoAlimentare(paziente.getId(), professionistaId,
                visitaOpt.map(Visita::getId).orElse(null), request.nome(), request.modalita());

        if (visitaOpt.isPresent()) {
            Visita visita = visitaOpt.get();
            Plicometria plicometria = plicometriaRepository.findByVisitaId(visita.getId()).orElse(null);
            Optional<SuggerimentoTdee> suggerimento = calcolatoreTdee.calcola(paziente, visita, plicometria);
            suggerimento.ifPresent(s -> {
                piano.setObiettivoKcalSuggerito(s.calorieSuggerite());
                piano.setBmrCalcolato(s.bmr());
                piano.setTdeeCalcolato(s.tdee());
                piano.setFormulaBmrUsata(s.formulaUsata());
                piano.setSottoSogliaSicurezza(s.sottoSogliaSicurezza());
                piano.setObiettivoKcal(s.calorieSuggerite());
            });
        }
        pianoAlimentareRepository.save(piano);

        switch (request.modalita()) {
            case PASTI -> creaPastiTemplate(piano.getId());
            case MACRO -> creaGiorniMacroTemplate(piano.getId());
            case ESEMPI -> creaEsempiTemplate(piano.getId());
        }

        return dettaglio(professionistaId, piano.getId());
    }

    private void creaPastiTemplate(UUID pianoId) {
        for (GiornoSettimana giorno : GiornoSettimana.values()) {
            int ordine = 0;
            for (TipoPasto tipo : PASTI_TEMPLATE) {
                pastoRepository.save(new Pasto(pianoId, giorno, NOMI_PASTI_TEMPLATE.get(tipo), tipo, null, ordine++));
            }
        }
    }

    private void creaGiorniMacroTemplate(UUID pianoId) {
        for (GiornoSettimana giorno : GiornoSettimana.values()) {
            pianoGiornoMacroTargetRepository.save(
                    new PianoGiornoMacroTarget(pianoId, giorno, null, null, null, null));
        }
    }

    private void creaEsempiTemplate(UUID pianoId) {
        int ordine = 0;
        for (TipoPasto tipo : PASTI_TEMPLATE) {
            pianoEsempioRepository.save(new PianoEsempio(pianoId, tipo, NOMI_PASTI_TEMPLATE.get(tipo), ordine++));
        }
    }

    private PianoAlimentare pianoDiProprieta(UUID professionistaId, UUID pianoId) {
        PianoAlimentare piano = pianoAlimentareRepository.findById(pianoId)
                .orElseThrow(PianoAlimentareNonTrovatoException::new);
        if (!piano.getProfessionistaId().equals(professionistaId)) {
            throw new PianoAlimentareNonTrovatoException();
        }
        return piano;
    }

    public PianoAlimentareResponse dettaglio(UUID professionistaId, UUID pianoId) {
        PianoAlimentare piano = pianoDiProprieta(professionistaId, pianoId);
        Paziente paziente = pazienteRepository.findById(piano.getPazienteId())
                .orElseThrow(PazienteNonTrovatoException::new);

        List<PastoResponse> pasti = List.of();
        List<GiornoMacroTargetResponse> giorniMacroTarget = List.of();
        List<EsempioResponse> esempi = List.of();

        if (piano.getModalita() == ModalitaPiano.PASTI) {
            // Il repository ordina per il valore stringa dell'enum (alfabetico): qui va trattato
            // solo come "prendi tutto" e riordinato in Java secondo l'ordine naturale dell'enum
            // (vedi ruling Task 1: GiornoSettimana/TipoPasto sono @Enumerated(EnumType.STRING)).
            List<Pasto> listaPasti = pastoRepository.findAllByPianoIdOrderByGiornoSettimanaAscOrdineAsc(pianoId);
            listaPasti = listaPasti.stream()
                    .sorted(Comparator.comparing(Pasto::getGiornoSettimana).thenComparing(Pasto::getOrdine))
                    .toList();
            List<UUID> pastoIds = listaPasti.stream().map(Pasto::getId).toList();
            Map<UUID, List<PianoAlimentoRiga>> righePerPasto = pianoAlimentoRigaRepository
                    .findAllByPastoIdInOrderByOrdineAsc(pastoIds).stream()
                    .collect(Collectors.groupingBy(PianoAlimentoRiga::getPastoId));
            pasti = listaPasti.stream()
                    .map(p -> PastoResponse.da(p, righePerPasto.getOrDefault(p.getId(), List.of())))
                    .toList();
        } else if (piano.getModalita() == ModalitaPiano.MACRO) {
            giorniMacroTarget = pianoGiornoMacroTargetRepository.findAllByPianoId(pianoId).stream()
                    .map(GiornoMacroTargetResponse::da).toList();
        } else {
            // Stessa nota: riordino in Java per lo stesso motivo (TipoPasto è @Enumerated(EnumType.STRING)).
            List<PianoEsempio> listaEsempi = pianoEsempioRepository.findAllByPianoIdOrderByTipoPastoAscOrdineAsc(pianoId);
            listaEsempi = listaEsempi.stream()
                    .sorted(Comparator.comparing(PianoEsempio::getTipoPasto).thenComparing(PianoEsempio::getOrdine))
                    .toList();
            List<UUID> esempioIds = listaEsempi.stream().map(PianoEsempio::getId).toList();
            Map<UUID, List<PianoAlimentoRiga>> righePerEsempio = pianoAlimentoRigaRepository
                    .findAllByEsempioIdInOrderByOrdineAsc(esempioIds).stream()
                    .collect(Collectors.groupingBy(PianoAlimentoRiga::getEsempioId));
            esempi = listaEsempi.stream()
                    .map(e -> EsempioResponse.da(e, righePerEsempio.getOrDefault(e.getId(), List.of())))
                    .toList();
        }

        return new PianoAlimentareResponse(piano.getId(), piano.getPazienteId(),
                paziente.getNome() + " " + paziente.getCognome(), piano.getNome(), piano.getModalita(),
                piano.statoEffettivo(), piano.getDataInizio(), piano.getDataFine(), piano.getObiettivoKcal(),
                piano.getObiettivoKcalSuggerito(), piano.getBmrCalcolato(), piano.getTdeeCalcolato(),
                piano.getFormulaBmrUsata(), piano.isSottoSogliaSicurezza(), pasti, giorniMacroTarget, esempi);
    }

    @Transactional
    public PianoAlimentareResponse aggiorna(UUID professionistaId, UUID pianoId, AggiornaPianoAlimentareRequest request) {
        PianoAlimentare piano = pianoDiProprieta(professionistaId, pianoId);
        piano.setNome(request.nome());
        piano.setDataFine(request.dataFine());
        piano.setObiettivoKcal(request.obiettivoKcal());
        pianoAlimentareRepository.save(piano);

        switch (piano.getModalita()) {
            case PASTI -> sostituisciPasti(pianoId, request.pasti() != null ? request.pasti() : List.of());
            case MACRO -> sostituisciGiorniMacro(pianoId,
                    request.giorniMacroTarget() != null ? request.giorniMacroTarget() : List.of());
            case ESEMPI -> sostituisciEsempi(pianoId, request.esempi() != null ? request.esempi() : List.of());
        }

        return dettaglio(professionistaId, pianoId);
    }

    private void sostituisciPasti(UUID pianoId, List<PastoRequest> richieste) {
        List<Pasto> esistenti = pastoRepository.findAllByPianoIdOrderByGiornoSettimanaAscOrdineAsc(pianoId);
        List<UUID> idEsistenti = esistenti.stream().map(Pasto::getId).toList();
        pianoAlimentoRigaRepository.deleteAllByPastoIdIn(idEsistenti);
        pastoRepository.deleteAllByPianoId(pianoId);

        int ordine = 0;
        for (PastoRequest richiesta : richieste) {
            Pasto pasto = pastoRepository.save(new Pasto(pianoId, richiesta.giornoSettimana(), richiesta.nome(),
                    richiesta.tipo(), richiesta.nota(), ordine++));
            salvaRighePerPasto(pasto.getId(), richiesta.righe());
        }
    }

    private void salvaRighePerPasto(UUID pastoId, List<RigaAlimentoRequest> righe) {
        if (righe == null) {
            return;
        }
        int ordineRiga = 0;
        for (RigaAlimentoRequest riga : righe) {
            pianoAlimentoRigaRepository.save(PianoAlimentoRiga.perPasto(pastoId, riga.alimentoId(), riga.nome(),
                    riga.kcal100g(), riga.proteine100g(), riga.carboidrati100g(), riga.grassi100g(),
                    riga.zuccheri100g(), riga.grammi(), ordineRiga++));
        }
    }

    private void sostituisciGiorniMacro(UUID pianoId, List<GiornoMacroTargetRequest> richieste) {
        pianoGiornoMacroTargetRepository.deleteAllByPianoId(pianoId);
        pianoGiornoMacroTargetRepository.flush();
        for (GiornoMacroTargetRequest richiesta : richieste) {
            pianoGiornoMacroTargetRepository.save(new PianoGiornoMacroTarget(pianoId, richiesta.giornoSettimana(),
                    richiesta.kcalTarget(), richiesta.proteineTarget(), richiesta.carboidratiTarget(),
                    richiesta.grassiTarget()));
        }
    }

    @Transactional
    public void attiva(UUID professionistaId, UUID pianoId) {
        PianoAlimentare piano = pianoDiProprieta(professionistaId, pianoId);
        pianoAlimentareRepository.findAllByPazienteIdAndStato(piano.getPazienteId(), StatoPiano.ATTIVO)
                .forEach(precedente -> {
                    precedente.setStato(StatoPiano.TERMINATO);
                    pianoAlimentareRepository.save(precedente);
                });
        piano.setStato(StatoPiano.ATTIVO);
        pianoAlimentareRepository.save(piano);
    }

    @Transactional
    public void elimina(UUID professionistaId, UUID pianoId) {
        PianoAlimentare piano = pianoDiProprieta(professionistaId, pianoId);
        if (piano.getStato() != StatoPiano.BOZZA) {
            throw new PianoAlimentareNonEliminabileException();
        }
        switch (piano.getModalita()) {
            case PASTI -> sostituisciPasti(pianoId, List.of());
            case MACRO -> sostituisciGiorniMacro(pianoId, List.of());
            case ESEMPI -> sostituisciEsempi(pianoId, List.of());
        }
        pianoAlimentareRepository.delete(piano);
    }

    public org.springframework.data.domain.Page<PianoAlimentare> cerca(UUID professionistaId,
            CriteriRicercaPianiAlimentari criteri, org.springframework.data.domain.Pageable pageable) {
        List<org.springframework.data.jpa.domain.Specification<PianoAlimentare>> specifiche = new java.util.ArrayList<>();
        specifiche.add(PianoAlimentareSpecifications.delProfessionista(professionistaId));
        if (criteri.ricerca() != null && !criteri.ricerca().isBlank()) {
            specifiche.add(PianoAlimentareSpecifications.conRicerca(criteri.ricerca()));
        }
        if (criteri.stato() != null) {
            specifiche.add(PianoAlimentareSpecifications.conStato(criteri.stato()));
        }
        if (criteri.pazienteId() != null) {
            specifiche.add(PianoAlimentareSpecifications.delPaziente(criteri.pazienteId()));
        }
        return pianoAlimentareRepository.findAll(
                org.springframework.data.jpa.domain.Specification.allOf(specifiche), pageable);
    }

    private void sostituisciEsempi(UUID pianoId, List<EsempioRequest> richieste) {
        List<PianoEsempio> esistenti = pianoEsempioRepository.findAllByPianoIdOrderByTipoPastoAscOrdineAsc(pianoId);
        List<UUID> idEsistenti = esistenti.stream().map(PianoEsempio::getId).toList();
        pianoAlimentoRigaRepository.deleteAllByEsempioIdIn(idEsistenti);
        pianoEsempioRepository.deleteAllByPianoId(pianoId);

        int ordine = 0;
        for (EsempioRequest richiesta : richieste) {
            PianoEsempio esempio = pianoEsempioRepository.save(
                    new PianoEsempio(pianoId, richiesta.tipoPasto(), richiesta.nome(), ordine++));
            if (richiesta.righe() != null) {
                int ordineRiga = 0;
                for (RigaAlimentoRequest riga : richiesta.righe()) {
                    pianoAlimentoRigaRepository.save(PianoAlimentoRiga.perEsempio(esempio.getId(), riga.alimentoId(),
                            riga.nome(), riga.kcal100g(), riga.proteine100g(), riga.carboidrati100g(),
                            riga.grassi100g(), riga.zuccheri100g(), riga.grammi(), ordineRiga++));
                }
            }
        }
    }
}
