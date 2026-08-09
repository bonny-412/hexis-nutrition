package com.hexisnutrition.backend;

import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BackendApplicationTest extends AbstractIntegrationTest {

    @Test
    void ilContestoSiAvviaCorrettamente() {
        assertThat(mockMvc).isNotNull();
    }
}
