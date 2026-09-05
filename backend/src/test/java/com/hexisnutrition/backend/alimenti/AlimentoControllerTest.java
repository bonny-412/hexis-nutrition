package com.hexisnutrition.backend.alimenti;

import com.hexisnutrition.backend.auth.JwtService;
import com.hexisnutrition.backend.auth.Ruolo;
import com.hexisnutrition.backend.professionisti.Professionista;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AlimentoControllerTest extends AbstractIntegrationTest {

    @Autowired
    private ProfessionistaRepository professionistaRepository;

    @Autowired
    private AlimentoRepository alimentoRepository;

    @Autowired
    private JwtService jwtService;

    // Righe BDA-shaped (professionista_id nullo) create da un test: pulite in @AfterEach
    // anche se un'asserzione del test fallisce, per non lasciare righe fasulle permanenti
    // nel database di test condiviso (alimenti è escluso dal TRUNCATE globale).
    private final List<UUID> bdaCreatiInQuestoTest = new ArrayList<>();

    @AfterEach
    void pulisci() {
        alimentoRepository.findAll().stream()
                .filter(a -> a.getProfessionistaId() != null)
                .forEach(alimentoRepository::delete);
        bdaCreatiInQuestoTest.forEach(id -> alimentoRepository.findById(id).ifPresent(alimentoRepository::delete));
        bdaCreatiInQuestoTest.clear();
        professionistaRepository.deleteAll();
    }

    private String tokenPer(Professionista professionista) {
        return jwtService.generateToken(professionista.getId(), Ruolo.PROFESSIONISTA);
    }

    private Alimento creaAlimento(UUID professionistaId, String nome, String categoria) {
        Alimento salvato = alimentoRepository.save(new Alimento(professionistaId, nome, categoria,
                new BigDecimal("100.00"), new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("5.00"),
                new BigDecimal("15.00"), null, null, null, null, null, null));
        if (professionistaId == null) {
            bdaCreatiInQuestoTest.add(salvato.getId());
        }
        return salvato;
    }

    @Test
    void dettaglioDiUnAlimentoBdaRestituisceIDatiConBdaTrue() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-alimenti1@example.com", "hash", "Anna", "Bianchi"));
        Alimento bda = creaAlimento(null, "Alimento BDA dettaglio1", "Categoria test");

        mockMvc.perform(get("/alimenti/" + bda.getId())
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Alimento BDA dettaglio1"))
                .andExpect(jsonPath("$.bda").value(true));
    }

    @Test
    void dettaglioDiUnAlimentoNonEsistenteRestituisce404() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-alimenti2@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(get("/alimenti/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isNotFound());
    }

    @Test
    void dettaglioDiUnAlimentoCustomDiUnAltroProfessionistaRestituisce404() throws Exception {
        Professionista professionistaA = professionistaRepository.save(
                new Professionista("prof-alimenti3a@example.com", "hash", "A", "A"));
        Professionista professionistaB = professionistaRepository.save(
                new Professionista("prof-alimenti3b@example.com", "hash", "B", "B"));
        Alimento diB = creaAlimento(professionistaB.getId(), "Alimento di B dettaglio3", "Personalizzato");

        mockMvc.perform(get("/alimenti/" + diB.getId())
                        .header("Authorization", "Bearer " + tokenPer(professionistaA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void ricercaSenzaAutenticazioneRestituisce401() throws Exception {
        mockMvc.perform(get("/alimenti/ricerca"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ricercaRestituisceGliAlimentiBdaEIPropriCustomNonQuelliAltrui() throws Exception {
        Professionista professionistaA = professionistaRepository.save(
                new Professionista("prof-alimenti4a@example.com", "hash", "A", "A"));
        Professionista professionistaB = professionistaRepository.save(
                new Professionista("prof-alimenti4b@example.com", "hash", "B", "B"));
        Alimento bda = creaAlimento(null, "Alimento BDA ricerca4", "Categoria ricerca4");
        creaAlimento(professionistaA.getId(), "Alimento di A ricerca4", "Categoria ricerca4");
        creaAlimento(professionistaB.getId(), "Alimento di B ricerca4", "Categoria ricerca4");

        mockMvc.perform(get("/alimenti/ricerca")
                        .param("ricerca", "ricerca4")
                        .header("Authorization", "Bearer " + tokenPer(professionistaA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto.length()").value(2))
                .andExpect(jsonPath("$.contenuto[*].nome",
                        org.hamcrest.Matchers.containsInAnyOrder("Alimento BDA ricerca4", "Alimento di A ricerca4")));
    }

    @Test
    void ricercaConFonteBdaRestituisceSoloAlimentiBda() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-alimenti5@example.com", "hash", "A", "A"));
        Alimento bda = creaAlimento(null, "Alimento BDA ricerca5", "Categoria ricerca5");
        creaAlimento(professionista.getId(), "Alimento custom ricerca5", "Categoria ricerca5");

        mockMvc.perform(get("/alimenti/ricerca")
                        .param("ricerca", "ricerca5")
                        .param("fonte", "BDA")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto.length()").value(1))
                .andExpect(jsonPath("$.contenuto[0].nome").value("Alimento BDA ricerca5"));
    }

    @Test
    void ricercaConFontePersonalizzatiRestituisceSoloIPropriAlimentiCustom() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-alimenti6@example.com", "hash", "A", "A"));
        Alimento bda = creaAlimento(null, "Alimento BDA ricerca6", "Categoria ricerca6");
        creaAlimento(professionista.getId(), "Alimento custom ricerca6", "Categoria ricerca6");

        mockMvc.perform(get("/alimenti/ricerca")
                        .param("ricerca", "ricerca6")
                        .param("fonte", "PERSONALIZZATI")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto.length()").value(1))
                .andExpect(jsonPath("$.contenuto[0].nome").value("Alimento custom ricerca6"));
    }

    @Test
    void ricercaPaginaIRisultati() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-alimenti7@example.com", "hash", "A", "A"));
        creaAlimento(professionista.getId(), "Alimento ricerca7 uno", "Categoria ricerca7");
        creaAlimento(professionista.getId(), "Alimento ricerca7 due", "Categoria ricerca7");
        creaAlimento(professionista.getId(), "Alimento ricerca7 tre", "Categoria ricerca7");

        mockMvc.perform(get("/alimenti/ricerca")
                        .param("ricerca", "ricerca7")
                        .param("dimensione", "2")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto.length()").value(2))
                .andExpect(jsonPath("$.totaleElementi").value(3))
                .andExpect(jsonPath("$.totalePagine").value(2));
    }

    @Test
    void ricercaConDirezioneDescOrdinaPerNomeDecrescente() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-alimenti19@example.com", "hash", "A", "A"));
        creaAlimento(professionista.getId(), "Alimento ricerca19 Anna", "Categoria ricerca19");
        creaAlimento(professionista.getId(), "Alimento ricerca19 Bruno", "Categoria ricerca19");
        creaAlimento(professionista.getId(), "Alimento ricerca19 Carlo", "Categoria ricerca19");

        mockMvc.perform(get("/alimenti/ricerca")
                        .param("ricerca", "ricerca19")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto[0].nome").value("Alimento ricerca19 Anna"))
                .andExpect(jsonPath("$.contenuto[2].nome").value("Alimento ricerca19 Carlo"));

        mockMvc.perform(get("/alimenti/ricerca")
                        .param("ricerca", "ricerca19")
                        .param("ordinaPer", "nome")
                        .param("direzione", "desc")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto[0].nome").value("Alimento ricerca19 Carlo"))
                .andExpect(jsonPath("$.contenuto[2].nome").value("Alimento ricerca19 Anna"));
    }

    @Test
    void creaUnAlimentoCustomRestituisce201ConBdaFalse() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-alimenti8@example.com", "hash", "A", "A"));

        mockMvc.perform(post("/alimenti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Frullato proteico fatto in casa",
                                  "categoria": "Bevande",
                                  "quantitaG": 100.0,
                                  "kcal": 180.0,
                                  "proteineG": 25.0,
                                  "grassiG": 3.5,
                                  "carboidratiG": 12.0
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Frullato proteico fatto in casa"))
                .andExpect(jsonPath("$.quantitaG").value(100.0))
                .andExpect(jsonPath("$.bda").value(false));
    }

    @Test
    void creaConQuantitaMancanteRestituisce400() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-alimenti16@example.com", "hash", "A", "A"));

        mockMvc.perform(post("/alimenti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Alimento senza quantita",
                                  "categoria": "Bevande",
                                  "kcal": 180.0,
                                  "proteineG": 25.0,
                                  "grassiG": 3.5,
                                  "carboidratiG": 12.0
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creaConUnaQuantitaDiRiferimentoDiversaDaCentoFunziona() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-alimenti17@example.com", "hash", "A", "A"));

        mockMvc.perform(post("/alimenti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Snack in busta da 30g",
                                  "categoria": "Snack",
                                  "quantitaG": 30.0,
                                  "kcal": 140.0,
                                  "proteineG": 2.0,
                                  "grassiG": 7.0,
                                  "carboidratiG": 18.0
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantitaG").value(30.0));
    }

    @Test
    void creaSenzaNomeRestituisce400() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-alimenti9@example.com", "hash", "A", "A"));

        mockMvc.perform(post("/alimenti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "",
                                  "categoria": "Bevande",
                                  "kcal": 180.0,
                                  "proteineG": 25.0,
                                  "grassiG": 3.5,
                                  "carboidratiG": 12.0
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aggiornaUnAlimentoCustomProprioFunziona() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-alimenti10@example.com", "hash", "A", "A"));
        Alimento custom = creaAlimento(professionista.getId(), "Alimento da modificare10", "Categoria10");

        mockMvc.perform(put("/alimenti/" + custom.getId())
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Alimento modificato10",
                                  "categoria": "Categoria10 modificata",
                                  "quantitaG": 50.0,
                                  "kcal": 200.0,
                                  "proteineG": 20.0,
                                  "grassiG": 8.0,
                                  "carboidratiG": 10.0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Alimento modificato10"))
                .andExpect(jsonPath("$.quantitaG").value(50.0));
    }

    @Test
    void aggiornareUnAlimentoBdaRestituisce409() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-alimenti11@example.com", "hash", "A", "A"));
        Alimento bda = creaAlimento(null, "Alimento BDA non modificabile11", "Categoria11");

        mockMvc.perform(put("/alimenti/" + bda.getId())
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Tentativo di modifica",
                                  "categoria": "Categoria11",
                                  "quantitaG": 100.0,
                                  "kcal": 200.0,
                                  "proteineG": 20.0,
                                  "grassiG": 8.0,
                                  "carboidratiG": 10.0
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void eliminaUnAlimentoCustomProprioFunziona() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-alimenti12@example.com", "hash", "A", "A"));
        Alimento custom = creaAlimento(professionista.getId(), "Alimento da eliminare12", "Categoria12");

        mockMvc.perform(delete("/alimenti/" + custom.getId())
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isNoContent());

        assertThat(alimentoRepository.findById(custom.getId())).isEmpty();
    }

    @Test
    void eliminareUnAlimentoBdaRestituisce409() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-alimenti13@example.com", "hash", "A", "A"));
        Alimento bda = creaAlimento(null, "Alimento BDA non eliminabile13", "Categoria13");

        mockMvc.perform(delete("/alimenti/" + bda.getId())
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isConflict());

        assertThat(alimentoRepository.findById(bda.getId())).isPresent();
    }

    @Test
    void duplicaUnAlimentoBdaCreaUnaCopiaPersonalizzata() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-alimenti14@example.com", "hash", "A", "A"));
        Alimento bda = creaAlimento(null, "Alimento BDA da duplicare14", "Categoria14");

        mockMvc.perform(post("/alimenti/" + bda.getId() + "/duplica")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Alimento BDA da duplicare14 (copia)"))
                .andExpect(jsonPath("$.bda").value(false));
    }

    @Test
    void nonSiPuoModificareUnAlimentoCustomDiUnAltroProfessionista() throws Exception {
        Professionista professionistaA = professionistaRepository.save(
                new Professionista("prof-alimenti15a@example.com", "hash", "A", "A"));
        Professionista professionistaB = professionistaRepository.save(
                new Professionista("prof-alimenti15b@example.com", "hash", "B", "B"));
        Alimento diB = creaAlimento(professionistaB.getId(), "Alimento di B15", "Categoria15");

        mockMvc.perform(put("/alimenti/" + diB.getId())
                        .header("Authorization", "Bearer " + tokenPer(professionistaA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Tentativo di modifica altrui",
                                  "categoria": "Categoria15",
                                  "quantitaG": 100.0,
                                  "kcal": 100.0,
                                  "proteineG": 10.0,
                                  "grassiG": 5.0,
                                  "carboidratiG": 15.0
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicaCopiaLaQuantitaDiRiferimentoDelloriginale() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-alimenti18@example.com", "hash", "A", "A"));
        Alimento bda = creaAlimento(null, "Alimento BDA da duplicare18", "Categoria18");

        mockMvc.perform(post("/alimenti/" + bda.getId() + "/duplica")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantitaG").value(bda.getQuantitaG().doubleValue()));
    }
}
