package ru.practicum.shareit.item;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.support.AbstractIntegrationTest;
import ru.practicum.shareit.user.User;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ItemControllerTest extends AbstractIntegrationTest {
    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    void shouldReturnEmptyArrayForBlankSearch(String text) throws Exception {
        User owner = user("Владелец");
        item(owner, "Дрель", true);
        mvc.perform(get("/items/search").header(USER_HEADER, owner.getId()).param("text", text))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldRejectMissingSearchParameter() throws Exception {
        mvc.perform(get("/items/search").header(USER_HEADER, 1))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldSearchAndListItems() throws Exception {
        User owner = user("Владелец");
        item(owner, "Дрель", true);
        mvc.perform(get("/items/search").header(USER_HEADER, owner.getId()).param("text", "ДРЕЛЬ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Дрель"));
        mvc.perform(get("/items").header(USER_HEADER, owner.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].comments").isEmpty());
    }

    @Test
    void shouldCreateAndPatchItem() throws Exception {
        User owner = user("Владелец");
        String result = mvc.perform(post("/items").header(USER_HEADER, owner.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Дрель\",\"description\":\"Для бетона\",\"available\":true}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(result).get("id").asLong();
        mvc.perform(patch("/items/{id}", id).header(USER_HEADER, owner.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"available\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.description").value("Для бетона"));
        mvc.perform(get("/items/{id}", id).header(USER_HEADER, owner.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"name\":\"\",\"description\":\"Описание\",\"available\":true}",
            "{\"name\":\"Дрель\",\"description\":\"   \",\"available\":true}",
            "{\"name\":\"Дрель\",\"description\":\"Описание\"}"
    })
    void shouldRejectInvalidItemCreation(String json) throws Exception {
        mvc.perform(post("/items").header(USER_HEADER, user("Владелец").getId())
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectBlankItemPatch() throws Exception {
        User owner = user("Владелец");
        Item stored = item(owner, "Дрель", true);
        mvc.perform(patch("/items/{id}", stored.getId()).header(USER_HEADER, owner.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingHeaderAndInvalidId() throws Exception {
        mvc.perform(get("/items")).andExpect(status().isBadRequest());
        mvc.perform(get("/items/not-a-number").header(USER_HEADER, 1))
                .andExpect(status().isBadRequest());
    }
}
