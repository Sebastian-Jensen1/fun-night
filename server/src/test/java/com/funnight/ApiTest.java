package com.funnight;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Plays through the real HTTP API, security filters included. */
@SpringBootTest(properties = "funnight.rooms.data-dir=")
@AutoConfigureMockMvc
class ApiTest {
    private static final String TOKEN = "X-Room-Token";

    @Autowired
    private MockMvc mvc;

    private String send(MockHttpServletRequestBuilder request, String token, String body, int expectedStatus) throws Exception {
        if (token != null) request.header(TOKEN, token);
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(body);
        return mvc.perform(request).andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString();
    }

    private String ok(MockHttpServletRequestBuilder request, String token, String body) throws Exception {
        return send(request, token, body, 200);
    }

    private static <T> T read(String json, String path) {
        return JsonPath.read(json, path);
    }

    @Test
    void listsSixGamesWithLockedDownHeaders() throws Exception {
        mvc.perform(get("/api/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].id").value("blackjack"))
                .andExpect(jsonPath("$[4].minPlayers").value(3))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void aWholeNightFromLobbyToRematch() throws Exception {
        String created = send(post("/api/rooms"), null,
                "{\"mode\":\"FULL\",\"playerNames\":[\"Anna\",\"Bo\"]}", 201);
        String id = read(created, "$.roomId");
        String host = read(created, "$.token");
        String code = read(created, "$.room.code");
        String rooms = "/api/rooms/" + id;

        // A phone scans the code, sees who's there and takes a new seat
        String lookup = ok(get("/api/rooms/lookup").param("code", code), null, null);
        assertEquals(2, (int) read(lookup, "$.seats.length()"));
        String joined = ok(post("/api/rooms/join"), null, "{\"code\":\"" + code + "\",\"name\":\"Cy\"}");
        String phone = read(joined, "$.token");
        assertEquals(3, (int) read(joined, "$.room.you.playerId"));
        ok(post(rooms + "/players"), host, "{\"name\":\"Dee\"}");

        // Live updates open as a stream
        mvc.perform(get(rooms + "/events").header(TOKEN, phone))
                .andExpect(request().asyncStarted())
                .andExpect(status().isOk());
        assertTrue((boolean) read(ok(get(rooms), host, null), "$.players[2].online"));

        // 1: blackjack. The phone bets for itself, not for others, and can't deal.
        send(post(rooms + "/start"), phone, null, 403);
        String state = ok(post(rooms + "/start"), host, null);
        assertEquals("blackjack", read(state, "$.currentGame.id"));
        ok(post(rooms + "/blackjack/bet"), phone, "{\"playerId\":3,\"sips\":4}");
        send(post(rooms + "/blackjack/bet"), phone, "{\"playerId\":1,\"sips\":4}", 403);
        send(post(rooms + "/blackjack/deal"), phone, null, 403);
        state = ok(post(rooms + "/blackjack/deal"), host, null);
        assertEquals(4, (int) read(state, "$.game.seats[2].stake"));
        while ("PLAYER_TURNS".equals(read(state, "$.game.phase"))) {
            assertEquals(1, (int) read(state, "$.game.dealer.cards.length()"), "hole card stays hidden");
            int current = read(state, "$.game.currentPlayerId");
            state = ok(post(rooms + "/blackjack/action"), host, "{\"playerId\":" + current + ",\"action\":\"STAND\"}");
        }
        assertEquals("ROUND_OVER", read(state, "$.game.phase"));
        send(post(rooms + "/dares/draw"), host, "{\"playerId\":1,\"level\":\"MILD\"}", 409);

        // 2: dare or drink
        state = ok(post(rooms + "/next"), host, null);
        assertEquals("dares", read(state, "$.currentGame.id"));
        ok(post(rooms + "/dares/draw"), host, "{\"playerId\":1,\"level\":\"WILD\"}");
        state = ok(post(rooms + "/dares/resolve"), host, "{\"playerId\":1,\"completed\":true}");
        int pending = read(state, "$.players[0].pendingGive");
        assertTrue(pending >= 3);
        state = ok(post(rooms + "/handout"), host, "{\"playerId\":1,\"targetId\":3,\"sips\":" + pending + "}");
        assertEquals(0, (int) read(state, "$.players[0].pendingGive"));

        // 3: higher or lower
        ok(post(rooms + "/next"), host, null);
        state = ok(post(rooms + "/higher-lower/guess"), host, "{\"playerId\":1,\"guess\":\"HIGHER\"}");
        assertTrue((int) read(state, "$.game.pile") >= 1);

        // 4: ring of fire
        ok(post(rooms + "/next"), host, null);
        state = ok(post(rooms + "/ring-of-fire/draw"), host, "{\"playerId\":1}");
        String target = "NONE".equals(read(state, "$.game.rule.target")) ? "" : ",\"targetId\":2";
        state = ok(post(rooms + "/ring-of-fire/resolve"), host, "{\"playerId\":1" + target + "}");
        assertEquals(51, (int) read(state, "$.game.cardsLeft"));

        // 5: guilty. Votes are secret until everyone is in.
        state = ok(post(rooms + "/next"), host, null);
        assertEquals("guilty", read(state, "$.currentGame.id"));
        state = ok(post(rooms + "/guilty/vote"), phone, "{\"playerId\":3,\"accusedId\":1}");
        assertEquals(1, (int) read(state, "$.game.yourVote"));
        state = ok(get(rooms), host, null);
        assertNull(read(state, "$.game.yourVote"));
        assertNull(read(state, "$.game.verdict"));
        for (int voter : new int[]{1, 2, 4}) {
            state = ok(post(rooms + "/guilty/vote"), host, "{\"playerId\":" + voter + ",\"accusedId\":1}");
        }
        assertEquals("VERDICT", read(state, "$.game.phase"));
        assertEquals(4, (int) read(state, "$.game.verdict.sips"));

        // 6: derby
        state = ok(post(rooms + "/next"), host, null);
        assertEquals("derby", read(state, "$.currentGame.id"));
        assertNull(read(state, "$.nextGame"));
        ok(post(rooms + "/derby/bet"), phone, "{\"playerId\":3,\"suit\":\"HEARTS\",\"sips\":2}");
        state = ok(post(rooms + "/derby/start"), host, null);
        while ("RACING".equals(read(state, "$.game.phase"))) {
            state = ok(post(rooms + "/derby/flip"), phone, null);
        }
        assertEquals(1, (int) read(state, "$.game.payouts.length()"));

        // Final scores, then a rematch with the same crew
        state = ok(post(rooms + "/next"), host, null);
        assertEquals("FINISHED", read(state, "$.phase"));
        state = ok(post(rooms + "/rematch"), host, null);
        assertEquals("LOBBY", read(state, "$.phase"));
        assertEquals(4, (int) read(state, "$.players.length()"));
        assertEquals("PLAYER", read(ok(get(rooms), phone, null), "$.you.role"));

        send(delete(rooms), phone, null, 403);
        send(delete(rooms), host, null, 204);
        send(get(rooms), host, null, 404);
    }

    @Test
    void rejectsMissingOrWrongTokensAndBadInput() throws Exception {
        String created = send(post("/api/rooms"), null, "{\"mode\":\"QUICKPLAY\",\"gameId\":\"blackjack\"}", 201);
        String rooms = "/api/rooms/" + read(created, "$.roomId");
        send(get(rooms), null, null, 403);
        send(get(rooms), "not-the-token", null, 403);
        send(get(rooms + "/events"), "not-the-token", null, 403);
        send(get("/api/rooms/nope"), "x", null, 404);
        send(post("/api/rooms"), null, "{\"mode\":\"PARTY\"}", 400);
        send(post("/api/rooms"), null, "{\"mode\":\"QUICKPLAY\"}", 400);
        send(post("/api/rooms"), null, "not json", 400);
        send(post("/api/rooms/join"), null, "{\"code\":\"ZZZZZ\",\"name\":\"Cy\"}", 404);
        send(post("/api/rooms/join"), null, "{\"name\":\"Cy\"}", 400);
        send(put(rooms), "x", null, 405);
        // No leaked internals in errors
        mvc.perform(post("/api/rooms").contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"PARTY\"}"))
                .andExpect(jsonPath("$.detail").value("Malformed request body."))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void appRoutesLoadTheWebApp() throws Exception {
        mvc.perform(get("/join/ABCDE")).andExpect(forwardedUrl("/index.html"));
        mvc.perform(get("/room/some-id")).andExpect(forwardedUrl("/index.html"))
                .andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.containsString("default-src 'self'")));
    }
}
