package com.hexisnutrition.backend.pazienti;

import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
public class JacksonPollock7Calcolatore implements CalcolatorePlicometria {

    @Override
    public ProtocolloPlicometrico protocollo() {
        return ProtocolloPlicometrico.JACKSON_POLLOCK_7;
    }

    @Override
    public Set<CampoPlica> plicheRichieste(Sesso sesso) {
        return EnumSet.of(CampoPlica.PETTORALE, CampoPlica.ASCELLARE, CampoPlica.TRICIPITALE,
                CampoPlica.SOTTOSCAPOLARE, CampoPlica.ADDOMINALE, CampoPlica.SOPRAILIACA, CampoPlica.COSCIA);
    }

    @Override
    public RisultatoDensita calcola(PlicheInput pliche, ContestoPlicometria contesto) {
        double s7 = CalcoliPlicometria.somma(pliche.pettoraleMm(), pliche.ascellareMm(), pliche.tricipitaleMm(),
                pliche.sottoscapolareMm(), pliche.addominaleMm(), pliche.soprailiacaMm(), pliche.cosciaMm());

        double eta = contesto.etaAnni();
        double d = contesto.sesso() == Sesso.M
                ? 1.112 - 0.00043499 * s7 + 0.00000055 * s7 * s7 - 0.00028826 * eta
                : 1.097 - 0.00046971 * s7 + 0.00000056 * s7 * s7 - 0.00012828 * eta;

        double percentualeGrasso = CalcoliPlicometria.percentualeGrassoSiri(d);

        return new RisultatoDensita(
                CalcoliPlicometria.arrotonda(s7, 2), CalcoliPlicometria.arrotonda(d, 4),
                CalcoliPlicometria.arrotonda(percentualeGrasso, 2), null, null,
                "jackson-pollock-1978-7siti");
    }
}
