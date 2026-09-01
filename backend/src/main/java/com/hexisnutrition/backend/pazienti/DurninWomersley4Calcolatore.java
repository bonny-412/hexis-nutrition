package com.hexisnutrition.backend.pazienti;

import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
public class DurninWomersley4Calcolatore implements CalcolatorePlicometria {

    private final DurninWomersleyCoefficienteRepository coefficienti;

    public DurninWomersley4Calcolatore(DurninWomersleyCoefficienteRepository coefficienti) {
        this.coefficienti = coefficienti;
    }

    @Override
    public ProtocolloPlicometrico protocollo() {
        return ProtocolloPlicometrico.DURNIN_WOMERSLEY_4;
    }

    @Override
    public Set<CampoPlica> plicheRichieste(Sesso sesso) {
        return EnumSet.of(CampoPlica.BICIPITALE, CampoPlica.TRICIPITALE, CampoPlica.SOTTOSCAPOLARE, CampoPlica.SOPRAILIACA);
    }

    @Override
    public RisultatoDensita calcola(PlicheInput pliche, ContestoPlicometria contesto) {
        double s4 = CalcoliPlicometria.somma(pliche.bicipitaleMm(), pliche.tricipitaleMm(),
                pliche.sottoscapolareMm(), pliche.soprailiacaMm());

        DurninWomersleyCoefficiente coefficiente = coefficienti.trovaCandidati(contesto.sesso(), contesto.etaAnni())
                .stream()
                .findFirst()
                .orElseThrow(() -> new CoefficientiDurninMancantiException(
                        "Nessun coefficiente Durnin-Womersley per sesso " + contesto.sesso()
                                + " ed età " + contesto.etaAnni()));

        double d = coefficiente.getC().doubleValue() - coefficiente.getM().doubleValue() * Math.log10(s4);
        double percentualeGrasso = CalcoliPlicometria.percentualeGrassoSiri(d);

        return new RisultatoDensita(
                CalcoliPlicometria.arrotonda(s4, 2), CalcoliPlicometria.arrotonda(d, 4),
                CalcoliPlicometria.arrotonda(percentualeGrasso, 2),
                coefficiente.getC(), coefficiente.getM(), "durnin-womersley-1974");
    }
}
