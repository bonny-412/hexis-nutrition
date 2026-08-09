package com.hexisnutrition.backend.support;

import com.hexisnutrition.backend.email.FakeEmailSender;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestEmailConfig {

    @Bean
    @Primary
    public FakeEmailSender fakeEmailSender() {
        return new FakeEmailSender();
    }
}
