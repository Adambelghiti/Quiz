package com.march.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Registration and login rules:
 * <ul>
 *   <li>usernames are compared case-insensitively, so a duplicate registration
 *       is a 409 Conflict rather than a second account or a 500;</li>
 *   <li>usernames have a length and character set;</li>
 *   <li>a duplicate is detected regardless of the case used.</li>
 * </ul>
 * The tests run in a transaction that is rolled back, so the accounts they
 * create do not leak into other test classes.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void aValidRegistrationReturnsAToken() throws Exception {
        mvc.perform(register("fresh-user", "secret"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("fresh-user"));
    }

    @Test
    void duplicateRegistrationInDifferentCaseIsAConflict() throws Exception {
        mvc.perform(register("Case-User", "secret")).andExpect(status().isCreated());

        mvc.perform(register("case-user", "secret"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("already exists")));
    }

    @Test
    void duplicateRegistrationInTheSameCaseIsAConflict() throws Exception {
        mvc.perform(register("same-case-user", "secret")).andExpect(status().isCreated());

        mvc.perform(register("same-case-user", "secret"))
                .andExpect(status().isConflict());
    }

    @Test
    void tooShortAUsernameIsRejected() throws Exception {
        mvc.perform(register("ab", "secret"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Username")));
    }

    @Test
    void aUsernameWithIllegalCharactersIsRejected() throws Exception {
        mvc.perform(register("bad name!", "secret"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginIsCaseInsensitive() throws Exception {
        mvc.perform(register("Mixed-Case-User", "secret")).andExpect(status().isCreated());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"mixed-case-user\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    /* ------------------------------------------------------------------ */

    private RequestBuilder register(String username, String password) {
        return post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
    }
}
