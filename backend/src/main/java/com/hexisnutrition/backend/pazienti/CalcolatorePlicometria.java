package com.hexisnutrition.backend.pazienti;

import java.util.Set;

public interface CalcolatorePlicometria {

    ProtocolloPlicometrico protocollo();

    Set<CampoPlica> plicheRichieste(Sesso sesso);

    RisultatoDensita calcola(PlicheInput pliche, ContestoPlicometria contesto);
}
