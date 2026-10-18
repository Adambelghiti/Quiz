package com.march.backend;

import com.march.backend.model.AuthUser;
import com.march.backend.repository.AuthUserRepository;
import com.march.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The game filters used to swallow anything they did not recognise and behave as
 * if no filter had been given, so a typo silently produced a game mixing
 * languages or categories. They are now rejected.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class GameStartFilterTest {

    private static final String AUTH = "Authorization";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private AuthUserRepository authUserRepository;

    @Test
    void anUnknownCategoryIsRejected() throws Exception {
        mvc.perform(start("filter-unknown-category", "{\"category\":\"NOT_A_CATEGORY\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("NOT_A_CATEGORY")))
                .andExpect(jsonPath("$.message").value(containsString("ISLAMIC_CULTURE")));
    }

    @Test
    void anUnknownLanguageIsRejected() throws Exception {
        mvc.perform(start("filter-unknown-language", "{\"language\":\"DE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Valid values are:")));
    }

    @Test
    void aMissingFilterStillMeansAny() throws Exception {
        mvc.perform(start("filter-no-values", "{\"rounds\":1}"))
                .andExpect(status().isOk());
    }

    @Test
    void aBlankFilterStillMeansAny() throws Exception {
        mvc.perform(start("filter-blank-values", "{\"category\":\"  \",\"language\":\"\",\"rounds\":1}"))
                .andExpect(status().isOk());
    }

    @Test
    void aFilterValueIsCaseInsensitive() throws Exception {
        mvc.perform(start("filter-lowercase", "{\"language\":\"fr\",\"rounds\":1}"))
                .andExpect(status().isOk());
    }

    /* ------------------------------------------------------------------ */

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder start(String username,
                                                                                             String body) {
        return post("/api/games/start")
                .header(AUTH, bearer(username))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    /** Register a user and mint a real signed token for it. */
    private String bearer(String username) {
        AuthUser user = new AuthUser();
        user.setUsername(username);
        user.setPassword("not-used-in-tests");
        user.setRole("USER");
        authUserRepository.save(user);
        return "Bearer " + jwtService.generateToken(username, "USER");
    }
}
