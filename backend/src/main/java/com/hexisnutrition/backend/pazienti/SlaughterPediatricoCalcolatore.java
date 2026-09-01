package com.hexisnutrition.backend.pazienti;

import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
public class SlaughterPediatricoCalcolatore implements CalcolatorePlicometria {

    @Override
    public ProtocolloPlicometrico protocollo() {
        return ProtocolloPlicometrico.SLAUGHTER_PEDIATRICO;
    }

    @Override
    public Set<CampoPlica> plicheRichieste(Sesso sesso) {
        return EnumSet.of(CampoPlica.TRICIPITALE, CampoPlica.POLPACCIO);
    }

    @Override
    public RisultatoDensita calcola(PlicheInput pliche, ContestoPlicometria contesto) {
        double s2 = CalcoliPlicometria.somma(pliche.tricipitaleMm(), pliche.polpaccioMm());

        double percentualeGrasso;
        if (contesto.sesso() == Sesso.M) {
            percentualeGrasso = s2 < 35
                    ? 0.735 * s2 + 1.0
                    : 1.21 * s2 - 0.008 * s2 * s2 - 1.7;
        } else {
            percentualeGrasso = s2 < 35
                    ? 0.610 * s2 + 5.1
                    : 1.21 * s2 - 0.008 * s2 * s2 - 3.4;
        }

        return new RisultatoDensita(
                CalcoliPlicometria.arrotonda(s2, 2), null,
                CalcoliPlicometria.arrotonda(percentualeGrasso, 2), null, null, "slaughter-1988");
    }
}
