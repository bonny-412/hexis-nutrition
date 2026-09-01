package com.hexisnutrition.backend.pazienti;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Period;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PlicometriaService {

    private final List<CalcolatorePlicometria> calcolatori;
    private final PlicometriaRepository plicometriaRepository;

    public PlicometriaService(List<CalcolatorePlicometria> calcolatori, PlicometriaRepository plicometriaRepository) {
        this.calcolatori = calcolatori;
        this.plicometriaRepository = plicometriaRepository;
    }

    public void elabora(Paziente paziente, Visita visita, PlicometriaRequest request) {
        if (request == null || request.protocollo() == null) {
            return;
        }
        if (paziente.getSesso() == Sesso.ALTRO) {
            throw new PlicometriaNonDisponibilePerSessoException();
        }

        CalcolatorePlicometria calcolatore = calcolatori.stream()
                .filter(c -> c.protocollo() == request.protocollo())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Nessun calcolatore per " + request.protocollo()));

        Set<CampoPlica> richieste = calcolatore.plicheRichieste(paziente.getSesso());
        Map<CampoPlica, BigDecimal> valori = valoriPerCampo(request);
        for (CampoPlica campo : richieste) {
            if (valori.get(campo) == null) {
                throw new PlicheMancantiException(
                        "Plica " + campo + " obbligatoria per il protocollo " + request.protocollo());
            }
        }

        int etaAnni = Period.between(paziente.getDataNascita(), visita.getDataVisita()).getYears();
        EtniaAtleta etnia = request.etniaAtleta() != null ? request.etniaAtleta() : EtniaAtleta.CAUCASICO;
        ContestoPlicometria contesto = new ContestoPlicometria(paziente.getSesso(), etaAnni, etnia);

        PlicheInput pliche = new PlicheInput(request.plicaPettoraleMm(), request.plicaAscellareMm(),
                request.plicaTricipitaleMm(), request.plicaBicipitaleMm(), request.plicaSottoscapolareMm(),
                request.plicaSoprailiacaMm(), request.plicaAddominaleMm(), request.plicaCosciaMm(),
                request.plicaPolpaccioMm());

        RisultatoDensita risultato = calcolatore.calcola(pliche, contesto);

        double percentualeGrassoGrezza = risultato.percentualeGrasso().doubleValue();
        boolean limiteSicurezzaApplicato = CalcoliPlicometria.limiteSicurezzaApplicato(percentualeGrassoGrezza, paziente.getSesso());
        double percentualeGrassoConLimite = CalcoliPlicometria.applicaLimiteSicurezza(percentualeGrassoGrezza, paziente.getSesso());
        BigDecimal percentualeGrasso = CalcoliPlicometria.arrotonda(percentualeGrassoConLimite, 2);

        BigDecimal massaGrassaKg = visita.getPesoKg().multiply(percentualeGrasso)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal massaMagraKg = visita.getPesoKg().subtract(massaGrassaKg);

        BigDecimal altezzaM = BigDecimal.valueOf(visita.getAltezzaCm()).divide(BigDecimal.valueOf(100));
        BigDecimal altezzaM2 = altezzaM.multiply(altezzaM);
        BigDecimal fmi = massaGrassaKg.divide(altezzaM2, 2, RoundingMode.HALF_UP);
        BigDecimal ffmi = massaMagraKg.divide(altezzaM2, 2, RoundingMode.HALF_UP);

        Plicometria plicometria = new Plicometria(visita.getId(), request.protocollo(), risultato.formulaVersione(),
                etaAnni, risultato.coefficienteC(), risultato.coefficienteM(),
                request.protocollo() == ProtocolloPlicometrico.EVANS_ATLETI ? etnia : null,
                valorePerScrittura(richieste, CampoPlica.PETTORALE, request.plicaPettoraleMm()),
                valorePerScrittura(richieste, CampoPlica.ASCELLARE, request.plicaAscellareMm()),
                valorePerScrittura(richieste, CampoPlica.TRICIPITALE, request.plicaTricipitaleMm()),
                valorePerScrittura(richieste, CampoPlica.BICIPITALE, request.plicaBicipitaleMm()),
                valorePerScrittura(richieste, CampoPlica.SOTTOSCAPOLARE, request.plicaSottoscapolareMm()),
                valorePerScrittura(richieste, CampoPlica.SOPRAILIACA, request.plicaSoprailiacaMm()),
                valorePerScrittura(richieste, CampoPlica.ADDOMINALE, request.plicaAddominaleMm()),
                valorePerScrittura(richieste, CampoPlica.COSCIA, request.plicaCosciaMm()),
                valorePerScrittura(richieste, CampoPlica.POLPACCIO, request.plicaPolpaccioMm()),
                risultato.sommaPlicheMm(), risultato.densitaCorporea(), percentualeGrasso,
                massaGrassaKg, massaMagraKg, fmi, ffmi, limiteSicurezzaApplicato);

        plicometriaRepository.save(plicometria);

        if (richieste.contains(CampoPlica.TRICIPITALE) && request.plicaTricipitaleMm() != null) {
            VisitaCalcoli.applicaMamc(visita, request.plicaTricipitaleMm());
        }
    }

    private BigDecimal valorePerScrittura(Set<CampoPlica> richieste, CampoPlica campo, BigDecimal valore) {
        return richieste.contains(campo) ? valore : null;
    }

    private Map<CampoPlica, BigDecimal> valoriPerCampo(PlicometriaRequest request) {
        Map<CampoPlica, BigDecimal> valori = new EnumMap<>(CampoPlica.class);
        valori.put(CampoPlica.PETTORALE, request.plicaPettoraleMm());
        valori.put(CampoPlica.ASCELLARE, request.plicaAscellareMm());
        valori.put(CampoPlica.TRICIPITALE, request.plicaTricipitaleMm());
        valori.put(CampoPlica.BICIPITALE, request.plicaBicipitaleMm());
        valori.put(CampoPlica.SOTTOSCAPOLARE, request.plicaSottoscapolareMm());
        valori.put(CampoPlica.SOPRAILIACA, request.plicaSoprailiacaMm());
        valori.put(CampoPlica.ADDOMINALE, request.plicaAddominaleMm());
        valori.put(CampoPlica.COSCIA, request.plicaCosciaMm());
        valori.put(CampoPlica.POLPACCIO, request.plicaPolpaccioMm());
        return valori;
    }
}
