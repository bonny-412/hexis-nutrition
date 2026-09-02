package com.hexisnutrition.backend.email;

import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

public class FakeEmailSender implements EmailSender {

    private final List<EmailInviata> inviate = new ArrayList<>();
    private boolean fallisce = false;

    @Override
    public void invia(String destinatario, String oggetto, String corpoHtml) {
        if (fallisce) {
            throw new RestClientException("Simulato: invio email non riuscito");
        }
        inviate.add(new EmailInviata(destinatario, oggetto, corpoHtml));
    }

    public List<EmailInviata> getInviate() {
        return inviate;
    }

    public void simulaFallimento() {
        fallisce = true;
    }

    public void reset() {
        inviate.clear();
        fallisce = false;
    }

    public record EmailInviata(String destinatario, String oggetto, String corpoHtml) {
    }
}
