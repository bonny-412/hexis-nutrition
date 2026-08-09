package com.hexisnutrition.backend.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class ResendEmailSender implements EmailSender {

    private final RestClient restClient;
    private final String fromEmail;

    public ResendEmailSender(RestClient.Builder restClientBuilder,
                              @Value("${resend.base-url}") String baseUrl,
                              @Value("${resend.api-key}") String apiKey,
                              @Value("${resend.from-email}") String fromEmail) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.fromEmail = fromEmail;
    }

    @Override
    public void invia(String destinatario, String oggetto, String corpoHtml) {
        restClient.post()
                .uri("/emails")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RichiestaEmailResend(fromEmail, List.of(destinatario), oggetto, corpoHtml))
                .retrieve()
                .toBodilessEntity();
    }

    private record RichiestaEmailResend(String from, List<String> to, String subject, String html) {
    }
}
