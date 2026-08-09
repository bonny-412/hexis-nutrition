package com.hexisnutrition.backend.email;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ResendEmailSenderTest {

    @Test
    void inviaChiamaLApiResendConIDatiCorretti() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer chiave-di-test"))
                .andExpect(jsonPath("$.from").value("no-reply@hexisnutrition.example"))
                .andExpect(jsonPath("$.to[0]").value("paziente@example.com"))
                .andExpect(jsonPath("$.subject").value("Oggetto di test"))
                .andRespond(withSuccess("{\"id\":\"abc123\"}", MediaType.APPLICATION_JSON));

        ResendEmailSender sender = new ResendEmailSender(builder, "https://api.resend.com",
                "chiave-di-test", "no-reply@hexisnutrition.example");

        sender.invia("paziente@example.com", "Oggetto di test", "<p>Corpo</p>");

        server.verify();
    }
}
