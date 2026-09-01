package com.hexisnutrition.backend.pazienti;

import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
public class JacksonPollock3Calcolatore implements CalcolatorePlicometria {

    @Override
    public ProtocolloPlicometrico protocollo() {
        return ProtocolloPlicometrico.JACKSON_POLLOCK_3;
    }

    @Override
    public Set<CampoPlica> plicheRichieste(Sesso sesso) {
        return sesso == Sesso.M
                ? EnumSet.of(CampoPlica.PETTORALE, CampoPlica.ADDOMINALE, CampoPlica.COSCIA)
                : EnumSet.of(CampoPlica.TRICIPITALE, CampoPlica.SOPRAILIACA, CampoPlica.COSCIA);
    }

    @Override
    public RisultatoDensita calcola(PlicheInput pliche, ContestoPlicometria contesto) {
        double s3 = contesto.sesso() == Sesso.M
                ? CalcoliPlicometria.somma(pliche.pettoraleMm(), pliche.addominaleMm(), pliche.cosciaMm())
                : CalcoliPlicometria.somma(pliche.tricipitaleMm(), pliche.soprailiacaMm(), pliche.cosciaMm());

        double eta = contesto.etaAnni();
        double d = contesto.sesso() == Sesso.M
                ? 1.109380 - 0.0008267 * s3 + 0.0000016 * s3 * s3 - 0.0002574 * eta
                : 1.0994921 - 0.0009929 * s3 + 0.0000023 * s3 * s3 - 0.0001392 * eta;

        double percentualeGrasso = CalcoliPlicometria.percentualeGrassoSiri(d);

        return new RisultatoDensita(
                CalcoliPlicometria.arrotonda(s3, 2), CalcoliPlicometria.arrotonda(d, 4),
                CalcoliPlicometria.arrotonda(percentualeGrasso, 2), null, null,
                "jackson-pollock-1978-3siti");
    }
}
