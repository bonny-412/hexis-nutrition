package com.hexisnutrition.backend.agenda;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;

public final class AgendaTempo {

    public static final ZoneId FUSO = ZoneId.of("Europe/Rome");
    public static final LocalTime INIZIO_GRIGLIA = LocalTime.of(7, 0);
    public static final LocalTime FINE_GRIGLIA = LocalTime.of(20, 0);

    public static final Set<Integer> ANTICIPI_NOTIFICA_MINUTI = Set.of(15, 30, 45, 60, 120);
    public static final int ANTICIPO_NOTIFICA_DEFAULT = 45;

    private AgendaTempo() {
    }

    public static LocalDateTime adesso() {
        return LocalDateTime.now(FUSO);
    }
}
