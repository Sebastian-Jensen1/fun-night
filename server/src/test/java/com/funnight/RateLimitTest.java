package com.funnight;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "funnight.rooms.data-dir=",
        "funnight.rate-limit.rooms-per-minute=3",
        "funnight.rate-limit.joins-per-minute=4"})
@AutoConfigureMockMvc
class RateLimitTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void limitsRoomCreationAndCodeGuessingPerClient() throws Exception {
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/rooms").contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"FULL\"}"))
                    .andExpect(status().isCreated());
        }
        mvc.perform(post("/api/rooms").contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"FULL\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.detail").exists());
        // A different client is unaffected
        mvc.perform(post("/api/rooms").with(request -> {
                    request.setRemoteAddr("203.0.113.9");
                    return request;
                }).contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"FULL\"}"))
                .andExpect(status().isCreated());

        for (int i = 0; i < 4; i++) {
            mvc.perform(get("/api/rooms/lookup").param("code", "ZZZZ" + i)).andExpect(status().isNotFound());
        }
        mvc.perform(get("/api/rooms/lookup").param("code", "ZZZZ9")).andExpect(status().isTooManyRequests());
        // Looking at the catalog is never limited
        mvc.perform(get("/api/games")).andExpect(status().isOk());
    }
}
