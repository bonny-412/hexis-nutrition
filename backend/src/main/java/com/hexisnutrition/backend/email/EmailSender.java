package com.hexisnutrition.backend.email;

public interface EmailSender {
    void invia(String destinatario, String oggetto, String corpoHtml);
}
