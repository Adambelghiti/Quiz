package com.march.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.march.backend.model.AuthUser;
import com.march.backend.repository.AuthUserRepository;
import com.march.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the question API against real HTTP: the facets endpoint used by the
 * setup screen, and the validation applied to admin writes.
 * <p>
 * Each test runs in a transaction that is rolled back, so the rows it creates do
 * not leak into the other test classes.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class QuestionApiTest {

    private static final String AUTH = "Authorization";

    private static final String VALID_QUESTION = "{"
            + "\"text\":\"Which colour is the sky on a clear day?\","
            + "\"category\":\"GENERAL_KNOWLEDGE\","
            + "\"difficulty\":1,"
            + "\"language\":\"EN\","
            + "\"correctAnswer\":\"Blue\","
            + "\"options\":[\"Blue\",\"Green\",\"Red\",\"Yellow\"],"
            + "\"hint\":\"Look up.\"}";

    private static final String QUESTION_WITHOUT_CATEGORY = "{"
            + "\"text\":\"No category on this one\","
            + "\"difficulty\":1,"
            + "\"language\":\"EN\","
            + "\"correctAnswer\":\"Blue\","
            + "\"options\":[\"Blue\",\"Green\"]}";

    private static final String QUESTION_WITH_ONE_OPTION = "{"
            + "\"text\":\"Only one option on this one\","
            + "\"category\":\"GENERAL_KNOWLEDGE\","
            + "\"difficulty\":1,"
            + "\"language\":\"EN\","
            + "\"correctAnswer\":\"Blue\","
            + "\"options\":[\"Blue\"]}";

    private static final String QUESTION_WITH_UNOFFERED_ANSWER = "{"
            + "\"text\":\"The answer is not offered on this one\","
            + "\"category\":\"GENERAL_KNOWLEDGE\","
            + "\"difficulty\":1,"
            + "\"language\":\"EN\","
            + "\"correctAnswer\":\"Purple\","
            + "\"options\":[\"Blue\",\"Green\"]}";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private AuthUserRepository authUserRepository;
    @Autowired
    private JdbcTemplate jdbc;

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void facetsOfferTheTwoNewCategories() throws Exception {
        String token = bearer("facets-user", "USER");

        JsonNode facets = readJson(authedGet("/api/questions/facets", token));
        List<String> categories = textValues(facets.get("categories"));

        assertTrue(categories.contains("ARABIC_CULTURE"),
                "the setup screen should offer ARABIC_CULTURE, got " + categories);
        assertTrue(categories.contains("ISLAMIC_CULTURE"),
                "the setup screen should offer ISLAMIC_CULTURE, got " + categories);
    }

    @Test
    void facetsSurviveARowWithoutCategoryOrLanguage() throws Exception {
        // A question stored before the columns were required. It cannot be saved
        // through JPA any more, so it is inserted the way legacy data would be.
        jdbc.update("insert into questions (difficulty) values (1)");
        Long legacyId = jdbc.queryForObject("select max(id) from questions", Long.class);
        String token = bearer("facets-legacy-user", "USER");
        try {
            mvc.perform(get("/api/questions/facets").header(AUTH, token))
                    .andExpect(status().isOk());
            mvc.perform(get("/api/questions").header(AUTH, token))
                    .andExpect(status().isOk());
        } finally {
            jdbc.update("delete from questions where id = ?", legacyId);
        }
    }

    @Test
    void anUnknownCategoryFilterIsAClientError() throws Exception {
        String token = bearer("badcategory-user", "USER");

        mvc.perform(get("/api/questions").param("category", "NOT_A_CATEGORY").header(AUTH, token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aQuestionWithoutACategoryIsRejected() throws Exception {
        String admin = bearer("question-admin", "ADMIN");

        mvc.perform(post("/api/questions")
                        .header(AUTH, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(QUESTION_WITHOUT_CATEGORY))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aQuestionWithFewerThanTwoOptionsIsRejected() throws Exception {
        String admin = bearer("question-admin", "ADMIN");

        mvc.perform(post("/api/questions")
                        .header(AUTH, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(QUESTION_WITH_ONE_OPTION))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aQuestionWhoseAnswerIsNotOfferedIsRejected() throws Exception {
        String admin = bearer("question-admin", "ADMIN");

        mvc.perform(post("/api/questions")
                        .header(AUTH, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(QUESTION_WITH_UNOFFERED_ANSWER))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aWellFormedQuestionIsAccepted() throws Exception {
        String admin = bearer("question-admin", "ADMIN");

        mvc.perform(post("/api/questions")
                        .header(AUTH, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_QUESTION))
                .andExpect(status().isOk());
    }

    @Test
    void onlyAdminsMayCreateQuestions() throws Exception {
        String user = bearer("question-ordinary-user", "USER");

        mvc.perform(post("/api/questions")
                        .header(AUTH, user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_QUESTION))
                .andExpect(status().isForbidden());
    }

    /* ------------------------------------------------------------------ */

    /** Register a user and mint a real signed token for it. */
    private String bearer(String username, String role) {
        AuthUser user = new AuthUser();
        user.setUsername(username);
        user.setPassword("not-used-in-tests");
        user.setRole(role);
        authUserRepository.save(user);
        return "Bearer " + jwtService.generateToken(username, role);
    }

    private JsonNode readJson(RequestBuilder request) throws Exception {
        String body = mvc.perform(request)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(body);
    }

    private MockHttpServletRequestBuilder authedGet(String url, String token) {
        return get(url).header(AUTH, token);
    }

    private List<String> textValues(JsonNode array) {
        List<String> values = new ArrayList<>();
        if (array != null) {
            array.forEach(node -> values.add(node.asText()));
        }
        return values;
    }
}
