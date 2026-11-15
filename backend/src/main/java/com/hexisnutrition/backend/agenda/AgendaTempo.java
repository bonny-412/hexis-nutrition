package com.hexisnutrition.backend.agenda;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public final class AgendaTempo {

    public static final ZoneId FUSO = ZoneId.of("Europe/Rome");
    public static final LocalTime INIZIO_GRIGLIA = LocalTime.of(8, 0);
    public static final LocalTime FINE_GRIGLIA = LocalTime.of(20, 0);

    private AgendaTempo() {
    }

    public static LocalDateTime adesso() {
        return LocalDateTime.now(FUSO);
    }
}
