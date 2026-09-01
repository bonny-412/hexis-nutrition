package com.hexisnutrition.backend.pazienti;

import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
public class Faulkner4Calcolatore implements CalcolatorePlicometria {

    @Override
    public ProtocolloPlicometrico protocollo() {
        return ProtocolloPlicometrico.FAULKNER_4;
    }

    @Override
    public Set<CampoPlica> plicheRichieste(Sesso sesso) {
        return EnumSet.of(CampoPlica.TRICIPITALE, CampoPlica.SOTTOSCAPOLARE, CampoPlica.SOPRAILIACA, CampoPlica.ADDOMINALE);
    }

    @Override
    public RisultatoDensita calcola(PlicheInput pliche, ContestoPlicometria contesto) {
        double s4 = CalcoliPlicometria.somma(pliche.tricipitaleMm(), pliche.sottoscapolareMm(),
                pliche.soprailiacaMm(), pliche.addominaleMm());

        double percentualeGrasso = s4 * 0.153 + 5.783;

        return new RisultatoDensita(
                CalcoliPlicometria.arrotonda(s4, 2), null,
                CalcoliPlicometria.arrotonda(percentualeGrasso, 2), null, null, "faulkner-1968");
    }
}
