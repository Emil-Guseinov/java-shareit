package ru.practicum.shareit.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import ru.practicum.shareit.support.AbstractIntegrationTest;
import ru.practicum.shareit.user.dto.UserDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest extends AbstractIntegrationTest {
    @Test
    void shouldCreateAndReadUser() throws Exception {
        String body = mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Эмиль\",\"email\":\"emil@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(body).get("id").asLong();
        mvc.perform(get("/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("emil@example.com"));
        mvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "not-an-email", "name@", "@example.com"})
    void shouldRejectInvalidEmailOnCreate(String email) throws Exception {
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserDto(null, "Имя", email))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "not-an-email"})
    void shouldRejectInvalidEmailOnPatch(String email) throws Exception {
        User existing = user("Имя");
        mvc.perform(patch("/users/{id}", existing.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserDto(null, null, email))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAllowPatchWithoutEmail() throws Exception {
        User existing = user("Имя");
        mvc.perform(patch("/users/{id}", existing.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Новое\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Новое"))
                .andExpect(jsonPath("$.email").value(existing.getEmail()));
    }

    @Test
    void shouldReturnConflictForDuplicateEmail() throws Exception {
        User existing = user("Имя");
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserDto(null, "Другой", existing.getEmail()))))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldDeleteUser() throws Exception {
        User existing = user("Имя");
        mvc.perform(delete("/users/{id}", existing.getId())).andExpect(status().isOk());
        mvc.perform(get("/users/{id}", existing.getId())).andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectMissingBodyAndMalformedJson() throws Exception {
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
    }
}
