package com.hexisnutrition.backend.alimenti;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AlimentoService {

    private final AlimentoRepository alimentoRepository;

    public AlimentoService(AlimentoRepository alimentoRepository) {
        this.alimentoRepository = alimentoRepository;
    }

    public Alimento dettaglio(UUID professionistaId, UUID alimentoId) {
        Alimento alimento = alimentoRepository.findById(alimentoId).orElseThrow(AlimentoNonTrovatoException::new);
        if (alimento.getProfessionistaId() != null && !alimento.getProfessionistaId().equals(professionistaId)) {
            throw new AlimentoNonTrovatoException();
        }
        return alimento;
    }

    public Page<Alimento> cerca(UUID professionistaId, CriteriRicercaAlimenti criteri, Pageable pageable) {
        List<Specification<Alimento>> specifiche = new ArrayList<>();
        specifiche.add(AlimentoSpecifications.visibilePer(professionistaId));
        if (criteri.ricerca() != null && !criteri.ricerca().isBlank()) {
            specifiche.add(AlimentoSpecifications.conRicerca(criteri.ricerca()));
        }
        if (criteri.fonte() == FonteAlimento.BDA) {
            specifiche.add(AlimentoSpecifications.soloBda());
        } else if (criteri.fonte() == FonteAlimento.PERSONALIZZATI) {
            specifiche.add(AlimentoSpecifications.soloPersonalizzati());
        }
        return alimentoRepository.findAll(Specification.allOf(specifiche), pageable);
    }

    @Transactional
    public Alimento crea(UUID professionistaId, CreaAlimentoRequest request) {
        Alimento alimento = new Alimento(professionistaId, request.nome(), request.categoria(), request.kcal(),
                request.proteineG(), request.grassiG(), request.carboidratiG(), request.acquaG(), request.fibreG(),
                request.zuccheriG(), request.ferroMg(), request.calcioMg(), request.sodioMg());
        return alimentoRepository.save(alimento);
    }

    @Transactional
    public Alimento aggiorna(UUID professionistaId, UUID alimentoId, AggiornaAlimentoRequest request) {
        Alimento alimento = dettaglio(professionistaId, alimentoId);
        if (alimento.isBda()) {
            throw new AlimentoNonModificabileException();
        }
        alimento.setNome(request.nome());
        alimento.setCategoria(request.categoria());
        alimento.setKcal(request.kcal());
        alimento.setProteineG(request.proteineG());
        alimento.setGrassiG(request.grassiG());
        alimento.setCarboidratiG(request.carboidratiG());
        alimento.setAcquaG(request.acquaG());
        alimento.setFibreG(request.fibreG());
        alimento.setZuccheriG(request.zuccheriG());
        alimento.setFerroMg(request.ferroMg());
        alimento.setCalcioMg(request.calcioMg());
        alimento.setSodioMg(request.sodioMg());
        return alimentoRepository.save(alimento);
    }

    @Transactional
    public void elimina(UUID professionistaId, UUID alimentoId) {
        Alimento alimento = dettaglio(professionistaId, alimentoId);
        if (alimento.isBda()) {
            throw new AlimentoNonModificabileException();
        }
        alimentoRepository.delete(alimento);
    }

    @Transactional
    public Alimento duplica(UUID professionistaId, UUID alimentoId) {
        Alimento originale = dettaglio(professionistaId, alimentoId);
        Alimento copia = new Alimento(professionistaId, originale.getNome() + " (copia)", originale.getCategoria(),
                originale.getKcal(), originale.getProteineG(), originale.getGrassiG(), originale.getCarboidratiG(),
                originale.getAcquaG(), originale.getFibreG(), originale.getZuccheriG(), originale.getFerroMg(),
                originale.getCalcioMg(), originale.getSodioMg());
        return alimentoRepository.save(copia);
    }
}
