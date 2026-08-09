package com.hexisnutrition.backend.email;

import java.util.ArrayList;
import java.util.List;

public class FakeEmailSender implements EmailSender {

    private final List<EmailInviata> inviate = new ArrayList<>();

    @Override
    public void invia(String destinatario, String oggetto, String corpoHtml) {
        inviate.add(new EmailInviata(destinatario, oggetto, corpoHtml));
    }

    public List<EmailInviata> getInviate() {
        return inviate;
    }

    public record EmailInviata(String destinatario, String oggetto, String corpoHtml) {
    }
}
