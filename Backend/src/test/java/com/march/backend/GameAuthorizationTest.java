package com.march.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.march.backend.model.AuthUser;
import com.march.backend.model.GameStatus;
import com.march.backend.model.Player;
import com.march.backend.repository.AuthUserRepository;
import com.march.backend.repository.GameRepository;
import com.march.backend.repository.PlayerRepository;
import com.march.backend.security.JwtService;
import com.march.backend.service.GameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level authorization tests.
 * <p>
 * These run against the real {@code SecurityConfig} filter chain (it is active in
 * the {@code test} profile too) with genuine JWTs, so they cover the rules that
 * production actually applies:
 * <ul>
 *   <li>anonymous requests are rejected with 401;</li>
 *   <li>game endpoints are scoped to the game's own participants;</li>
 *   <li>the legacy player CRUD is admin-only while {@code /players/me} stays open;</li>
 *   <li>a client can never assign {@code authUserId}.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class GameAuthorizationTest {

    private static final String AUTH = "Authorization";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private AuthUserRepository authUserRepository;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private GameRepository gameRepository;
    @Autowired
    private GameService gameService;

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void anonymousRequestsAreRejectedWith401() throws Exception {
        mvc.perform(get("/api/games")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/players/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void resultsAreOnlyVisibleToParticipants() throws Exception {
        String owner = bearer("results-owner", "USER");
        String stranger = bearer("results-stranger", "USER");
        long gameId = startSoloGame(owner, 1);
        gameService.finishGame(gameId);

        mvc.perform(get("/api/games/{id}/results", gameId).header(AUTH, owner))
                .andExpect(status().isOk());

        mvc.perform(get("/api/games/{id}/results", gameId).header(AUTH, stranger))
                .andExpect(status().isForbidden());
    }

    @Test
    void aGameCanOnlyBeFinishedByAParticipant() throws Exception {
        String owner = bearer("finish-owner", "USER");
        String stranger = bearer("finish-stranger", "USER");
        long gameId = startSoloGame(owner, 2);

        mvc.perform(post("/api/games/{id}/finish", gameId).header(AUTH, stranger))
                .andExpect(status().isForbidden());
        assertEquals(GameStatus.IN_PROGRESS, statusOf(gameId),
                "a non-participant must not be able to finish the game");

        mvc.perform(post("/api/games/{id}/finish", gameId).header(AUTH, owner))
                .andExpect(status().isOk());
        assertEquals(GameStatus.FINISHED, statusOf(gameId));
    }

    @Test
    void onlyParticipantsCanAdvanceARound() throws Exception {
        String owner = bearer("advance-owner", "USER");
        String stranger = bearer("advance-stranger", "USER");
        long gameId = startSoloGame(owner, 2);

        mvc.perform(post("/api/games/{id}/next-round", gameId).header(AUTH, stranger))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyParticipantsCanReadAGame() throws Exception {
        String owner = bearer("read-owner", "USER");
        String stranger = bearer("read-stranger", "USER");
        long gameId = startSoloGame(owner, 1);

        mvc.perform(get("/api/games/{id}", gameId).header(AUTH, owner))
                .andExpect(status().isOk());

        mvc.perform(get("/api/games/{id}", gameId).header(AUTH, stranger))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyParticipantsCanSubmitAnAnswer() throws Exception {
        String owner = bearer("answer-owner", "USER");
        String stranger = bearer("answer-stranger", "USER");
        long gameId = startSoloGame(owner, 2);
        Long questionId = gameRepository.findById(gameId).orElseThrow().getCurrentQuestionId();

        mvc.perform(post("/api/games/{id}/answer", gameId)
                        .header(AUTH, stranger)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":" + questionId + ",\"answer\":\"whatever\",\"elapsedMs\":0}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void legacyPlayerCrudIsAdminOnlyButSelfServiceStillWorks() throws Exception {
        String user = bearer("player-user", "USER");
        String admin = bearer("player-admin", "ADMIN");

        mvc.perform(get("/api/players").header(AUTH, user)).andExpect(status().isForbidden());
        mvc.perform(get("/api/players").header(AUTH, admin)).andExpect(status().isOk());

        mvc.perform(post("/api/players")
                        .header(AUTH, user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"should-not-exist\"}"))
                .andExpect(status().isForbidden());

        // Self-service endpoints must keep working for ordinary players.
        mvc.perform(get("/api/players/me").header(AUTH, user)).andExpect(status().isOk());
        mvc.perform(get("/api/players/me/stats").header(AUTH, user)).andExpect(status().isOk());
        mvc.perform(get("/api/players/me/history").header(AUTH, user)).andExpect(status().isOk());
    }

    @Test
    void clientsCannotAssignAuthUserId() throws Exception {
        String admin = bearer("hijack-admin", "ADMIN");

        mvc.perform(post("/api/players")
                        .header(AUTH, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"hijack-target\",\"authUserId\":987654}"))
                .andExpect(status().isOk());

        Player created = playerRepository.findAll().stream()
                .filter(p -> "hijack-target".equals(p.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the player should have been created"));
        assertNull(created.getAuthUserId(),
                "authUserId is read-only over JSON and must never be taken from the client");
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

    /** Start a solo game through the API and return its id. */
    private long startSoloGame(String token, int rounds) throws Exception {
        String response = mvc.perform(post("/api/games/start")
                        .header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rounds\":" + rounds + "}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        JsonNode node = json.readTree(response);
        return node.get("gameId").asLong();
    }

    private GameStatus statusOf(long gameId) {
        return gameRepository.findById(gameId).orElseThrow().getStatus();
    }
}
