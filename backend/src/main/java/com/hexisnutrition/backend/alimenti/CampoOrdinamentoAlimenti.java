package com.hexisnutrition.backend.alimenti;

public enum CampoOrdinamentoAlimenti {
    nome,
    /** Con una ricerca: prima i nomi che iniziano col testo, poi le parole che iniziano col testo, poi il resto; a parità, per nome. */
    pertinenza
}
