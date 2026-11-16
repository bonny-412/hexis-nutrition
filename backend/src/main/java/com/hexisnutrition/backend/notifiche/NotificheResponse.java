package com.hexisnutrition.backend.notifiche;

import java.util.List;

public record NotificheResponse(List<NotificaResponse> notifiche, long nonLette, long nonVisteInAgenda) {
}
