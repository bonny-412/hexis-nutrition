package com.hexisnutrition.backend.pazienti;

import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
public class EvansAtletiCalcolatore implements CalcolatorePlicometria {

    @Override
    public ProtocolloPlicometrico protocollo() {
        return ProtocolloPlicometrico.EVANS_ATLETI;
    }

    @Override
    public Set<CampoPlica> plicheRichieste(Sesso sesso) {
        return EnumSet.of(CampoPlica.TRICIPITALE, CampoPlica.ADDOMINALE, CampoPlica.COSCIA);
    }

    @Override
    public RisultatoDensita calcola(PlicheInput pliche, ContestoPlicometria contesto) {
        double skf3 = CalcoliPlicometria.somma(pliche.tricipitaleMm(), pliche.addominaleMm(), pliche.cosciaMm());

        double sesso = contesto.sesso() == Sesso.M ? 1 : 0;
        double etnia = contesto.etniaAtleta() == EtniaAtleta.AFROAMERICANO ? 1 : 0;

        double percentualeGrasso = 8.997 + 0.24658 * skf3 - 6.343 * sesso - 1.998 * etnia;

        return new RisultatoDensita(
                CalcoliPlicometria.arrotonda(skf3, 2), null,
                CalcoliPlicometria.arrotonda(percentualeGrasso, 2), null, null, "evans-2005");
    }
}
