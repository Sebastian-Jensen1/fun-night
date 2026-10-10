package com.funnight.backend.session;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SessionApiTest {

    @Autowired
    private MockMvc mvc;

    private String[] createQuickplay(String game) throws Exception {
        MvcResult result = mvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"QUICKPLAY\",\"gameId\":\"" + game + "\",\"playerNames\":[\"Seb\",\"Anna\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.session.players.length()").value(2))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        return new String[]{extract(body, "\"id\":\"([^\"]+)\""), extract(body, "\"token\":\"([^\"]+)\"")};
    }

    private static String extract(String body, String regex) {
        Matcher m = Pattern.compile(regex).matcher(body);
        if (!m.find()) throw new AssertionError("No match for " + regex + " in " + body);
        return m.group(1);
    }

    @Test
    void listsSixGames() throws Exception {
        mvc.perform(get("/api/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].id").value("blackjack"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void playsARoundOfBlackjack() throws Exception {
        String[] s = createQuickplay("blackjack");
        mvc.perform(post("/api/sessions/" + s[0] + "/blackjack/deal")
                        .header("X-Session-Token", s[1])
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bets\":{\"1\":2,\"2\":3}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.round").value(1))
                .andExpect(jsonPath("$.seats.length()").value(2));
    }

    @Test
    void rejectsMissingOrWrongToken() throws Exception {
        String[] s = createQuickplay("blackjack");
        mvc.perform(get("/api/sessions/" + s[0])).andExpect(status().isForbidden());
        mvc.perform(get("/api/sessions/" + s[0]).header("X-Session-Token", "nope")).andExpect(status().isForbidden());
        mvc.perform(get("/api/sessions/" + s[0]).header("X-Session-Token", s[1])).andExpect(status().isOk())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void cannotPlayAGameTheSessionIsNotOn() throws Exception {
        String[] s = createQuickplay("blackjack");
        mvc.perform(get("/api/sessions/" + s[0] + "/dares").header("X-Session-Token", s[1]))
                .andExpect(status().isConflict());
    }

    @Test
    void validatesInput() throws Exception {
        mvc.perform(post("/api/sessions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"FULL\",\"playerNames\":[]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/sessions").contentType(MediaType.APPLICATION_JSON).content("not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
