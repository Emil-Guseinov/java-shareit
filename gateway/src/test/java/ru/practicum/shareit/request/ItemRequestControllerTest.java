package ru.practicum.shareit.request;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MvcResult;
import ru.practicum.shareit.common.exception.ErrorResponse;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.support.ControllerTestSupport;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemRequestController.class)
class ItemRequestControllerTest extends ControllerTestSupport {
    @MockBean
    private ItemRequestClient service;

    @Test
    void createReturnsRequest() throws Exception {
        ItemRequestCreateDto request = new ItemRequestCreateDto("Нужна дрель");
        Map<String, Object> expected = Map.of("id", 3L, "description", "Нужна дрель",
                "items", List.of(Map.of("id", 2L, "name", "Дрель", "ownerId", 8L)));
        when(service.create(eq(1L), any(ItemRequestCreateDto.class))).thenReturn(ResponseEntity.ok(expected));
        MvcResult result = mvc.perform(post("/requests").header(HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isOk()).andReturn();
        assertBody(result, expected);
        verify(service).create(eq(1L), refEq(request));
    }

    @Test
    void getOwnReturnsRequestsWithAnswers() throws Exception {
        Map<String, Object> expected = Map.of("id", 3L, "description", "Нужна дрель",
                "items", List.of(Map.of("id", 2L, "name", "Дрель", "ownerId", 8L)));
        when(service.getOwn(1L)).thenReturn(ResponseEntity.ok(List.of(expected)));
        MvcResult result = mvc.perform(get("/requests").header(HEADER, 1)).andExpect(status().isOk()).andReturn();
        assertBody(result, List.of(expected));
        verify(service).getOwn(1L);
    }

    @Test
    void getOthersReturnsRequestsWithAnswers() throws Exception {
        Map<String, Object> expected = Map.of("id", 3L, "description", "Нужна дрель",
                "items", List.of(Map.of("id", 2L, "name", "Дрель", "ownerId", 8L)));
        when(service.getOthers(1L)).thenReturn(ResponseEntity.ok(List.of(expected)));
        MvcResult result = mvc.perform(get("/requests/all").header(HEADER, 1)).andExpect(status().isOk()).andReturn();
        assertBody(result, List.of(expected));
        verify(service).getOthers(1L);
    }

    @Test
    void getByIdReturnsRequestWithAnswers() throws Exception {
        Map<String, Object> expected = Map.of("id", 3L, "description", "Нужна дрель",
                "items", List.of(Map.of("id", 2L, "name", "Дрель", "ownerId", 8L)));
        when(service.getById(1L, 3L)).thenReturn(ResponseEntity.ok(expected));
        MvcResult result = mvc.perform(get("/requests/3").header(HEADER, 1)).andExpect(status().isOk()).andReturn();
        assertBody(result, expected);
        verify(service).getById(1L, 3L);
    }

    @Test
    void requiresHeaderForOwn() throws Exception {
        mvc.perform(get("/requests")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsInvalidHeaderForOwn() throws Exception {
        mvc.perform(get("/requests").header(HEADER, -1)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void requiresHeaderForOthers() throws Exception {
        mvc.perform(get("/requests/all")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsInvalidHeaderForOthers() throws Exception {
        mvc.perform(get("/requests/all").header(HEADER, -1)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsInvalidDescription(String description) throws Exception {
        mvc.perform(post("/requests").header(HEADER, 1).contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ItemRequestCreateDto(description))))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void doesNotForwardAuthorityFields() throws Exception {
        Map<String, Object> body = Map.of("description", "Нужна дрель", "id", 99L,
                "requesterId", 99L, "created", "2000-01-01T00:00:00");
        when(service.create(eq(1L), any(ItemRequestCreateDto.class))).thenReturn(ResponseEntity.ok(Map.of("id", 3L)));
        mvc.perform(post("/requests").header(HEADER, 1).contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isOk());
        verify(service).create(eq(1L), refEq(new ItemRequestCreateDto("Нужна дрель")));
    }

    @Test
    void preservesServerError() throws Exception {
        when(service.getById(1L, 3L)).thenReturn(ResponseEntity.status(404).body(new ErrorResponse("Нет запроса")));
        MvcResult result = mvc.perform(get("/requests/3").header(HEADER, 1))
                .andExpect(status().isNotFound()).andReturn();
        assertBody(result, new ErrorResponse("Нет запроса"));
    }

    @Test
    void rejectsInvalidRequestId() throws Exception {
        mvc.perform(get("/requests/-1").header(HEADER, 1)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
