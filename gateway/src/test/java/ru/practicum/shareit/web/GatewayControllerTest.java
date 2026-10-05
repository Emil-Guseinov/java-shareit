package ru.practicum.shareit.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.BookingClient;
import ru.practicum.shareit.booking.BookingController;
import ru.practicum.shareit.booking.BookingState;
import ru.practicum.shareit.common.exception.ServerUnavailableException;
import ru.practicum.shareit.item.ItemClient;
import ru.practicum.shareit.item.ItemController;
import ru.practicum.shareit.request.ItemRequestClient;
import ru.practicum.shareit.request.ItemRequestController;
import ru.practicum.shareit.user.UserClient;
import ru.practicum.shareit.user.UserController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({UserController.class, ItemController.class, BookingController.class, ItemRequestController.class})
class GatewayControllerTest {
    private static final String HEADER = "X-Sharer-User-Id";
    @Autowired
    private MockMvc mvc;
    @MockBean
    private UserClient users;
    @MockBean
    private ItemClient items;
    @MockBean
    private BookingClient bookings;
    @MockBean
    private ItemRequestClient requests;

    @Test
    void forwardsAllUserRoutesAndPartialUpdate() throws Exception {
        when(users.create(any())).thenReturn(ok("{\"id\":1}"));
        when(users.update(eq(1L), any())).thenReturn(ok("{\"id\":1}"));
        when(users.getById(1L)).thenReturn(ok("{\"id\":1}"));
        when(users.getAll()).thenReturn(ok("[]"));
        when(users.remove(1L)).thenReturn(ResponseEntity.noContent().build());
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Имя\",\"email\":\"test@example.com\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
        mvc.perform(patch("/users/1").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Новое\"}"))
                .andExpect(status().isOk());
        verify(users).update(eq(1L), argThat(dto -> dto.getEmail() == null && dto.getName().equals("Новое")));
        mvc.perform(get("/users/1")).andExpect(status().isOk());
        mvc.perform(get("/users")).andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(delete("/users/1")).andExpect(status().isNoContent());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"name\":\"Имя\"}", "{\"name\":\"\",\"email\":\"ok@example.com\"}",
            "{\"name\":\"Имя\",\"email\":\"bad\"}", "{\"name\":\"Имя\",\"email\":\"\"}"})
    void rejectsInvalidUserBeforeCallingServer(String body) throws Exception {
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").isNotEmpty());
        verifyNoInteractions(users);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "not-an-email"})
    void rejectsInvalidEmailPatch(String email) throws Exception {
        mvc.perform(patch("/users/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(users);
    }

    @Test
    void forwardsItemRoutesAndKeepsFalseInPatch() throws Exception {
        when(items.create(eq(1L), any())).thenReturn(ok("{\"id\":2,\"requestId\":3}"));
        when(items.update(eq(1L), eq(2L), any())).thenReturn(ok("{\"available\":false}"));
        when(items.getById(1L, 2L)).thenReturn(ok("{\"id\":2}"));
        when(items.getByOwner(1L)).thenReturn(ok("[]"));
        when(items.search(1L, "дрель + & %")).thenReturn(ok("[]"));
        when(items.addComment(eq(1L), eq(2L), any())).thenReturn(ok("{\"text\":\"Отзыв\"}"));
        mvc.perform(post("/items").header(HEADER, 1).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Дрель\",\"description\":\"Описание\",\"available\":true,\"requestId\":3}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.requestId").value(3));
        verify(items).create(eq(1L), argThat(dto -> Long.valueOf(3).equals(dto.getRequestId())));
        mvc.perform(patch("/items/2").header(HEADER, 1).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"available\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.available").value(false));
        verify(items).update(eq(1L), eq(2L), argThat(dto -> Boolean.FALSE.equals(dto.getAvailable()) && dto.getName() == null));
        mvc.perform(get("/items/2").header(HEADER, 1)).andExpect(status().isOk());
        mvc.perform(get("/items").header(HEADER, 1)).andExpect(status().isOk());
        mvc.perform(get("/items/search").header(HEADER, 1).param("text", "дрель + & %"))
                .andExpect(status().isOk());
        mvc.perform(post("/items/2/comment").header(HEADER, 1).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Отзыв\"}"))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    void blankSearchNeverCallsServer(String text) throws Exception {
        mvc.perform(get("/items/search").header(HEADER, 1).param("text", text))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        verifyNoInteractions(items);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"name\":\"\",\"description\":\"Описание\",\"available\":true}",
            "{\"name\":\"Дрель\",\"description\":\"   \",\"available\":true}",
            "{\"name\":\"Дрель\",\"description\":\"Описание\"}",
            "{\"name\":\"Дрель\",\"description\":\"Описание\",\"available\":true,\"requestId\":0}"})
    void rejectsInvalidItemBeforeForwarding(String body) throws Exception {
        mvc.perform(post("/items").header(HEADER, 1).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(items);
    }

    @Test
    void rejectsBlankItemPatchAndComment() throws Exception {
        mvc.perform(patch("/items/2").header(HEADER, 1).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/items/2/comment").header(HEADER, 1).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"   \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(items);
    }

    @Test
    void forwardsAllBookingRoutes() throws Exception {
        when(bookings.create(eq(1L), any())).thenReturn(ok("{\"id\":5}"));
        when(bookings.approve(1L, 5L, false)).thenReturn(ok("{\"status\":\"REJECTED\"}"));
        when(bookings.getById(1L, 5L)).thenReturn(ok("{\"id\":5}"));
        when(bookings.getByBooker(1L, BookingState.ALL)).thenReturn(ok("[]"));
        when(bookings.getByOwner(1L, BookingState.FUTURE)).thenReturn(ok("[]"));
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        mvc.perform(post("/bookings").header(HEADER, 1).contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(start, start.plusDays(1))))
                .andExpect(status().isOk());
        mvc.perform(patch("/bookings/5").header(HEADER, 1).param("approved", "false"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));
        mvc.perform(get("/bookings/5").header(HEADER, 1)).andExpect(status().isOk());
        mvc.perform(get("/bookings").header(HEADER, 1)).andExpect(status().isOk());
        mvc.perform(get("/bookings/owner").header(HEADER, 1).param("state", "FUTURE"))
                .andExpect(status().isOk());
    }

    @Test
    void invalidDatesAreRejectedBeforeForwarding() throws Exception {
        LocalDateTime past = LocalDateTime.now().minusDays(1);
        mvc.perform(post("/bookings").header(HEADER, 1).contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(past, past.plusDays(3))))
                .andExpect(status().isBadRequest());
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        mvc.perform(post("/bookings").header(HEADER, 1).contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(start, start)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/bookings").header(HEADER, 1).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(bookings);
    }

    @Test
    void validatesBookingQueryParameters() throws Exception {
        mvc.perform(get("/bookings").header(HEADER, 1).param("state", "UNKNOWN"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("Unknown state: UNKNOWN"));
        mvc.perform(get("/bookings/owner").header(HEADER, 1).param("state", "bad"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/bookings/5").header(HEADER, 1)).andExpect(status().isBadRequest());
        mvc.perform(patch("/bookings/5").header(HEADER, 1).param("approved", "not-boolean"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(bookings);
    }

    @Test
    void forwardsAllRequestRoutes() throws Exception {
        when(requests.create(eq(1L), any())).thenReturn(ok("{\"id\":3}"));
        when(requests.getOwn(1L)).thenReturn(ok("[]"));
        when(requests.getOthers(1L)).thenReturn(ok("[]"));
        when(requests.getById(1L, 3L)).thenReturn(ok("{\"id\":3}"));
        mvc.perform(post("/requests").header(HEADER, 1).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Нужна дрель\",\"requesterId\":99}"))
                .andExpect(status().isOk());
        verify(requests).create(eq(1L), argThat(dto -> dto.getDescription().equals("Нужна дрель")));
        mvc.perform(get("/requests").header(HEADER, 1)).andExpect(status().isOk());
        mvc.perform(get("/requests/all").header(HEADER, 1)).andExpect(status().isOk());
        mvc.perform(get("/requests/3").header(HEADER, 1)).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"description\":null}", "{\"description\":\"\"}", "{\"description\":\"   \"}"})
    void rejectsInvalidRequestBeforeServer(String body) throws Exception {
        mvc.perform(post("/requests").header(HEADER, 1).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(requests);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/items", "/bookings", "/requests", "/requests/all"})
    void validatesHeader(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isBadRequest());
        mvc.perform(get(path).header(HEADER, -1)).andExpect(status().isBadRequest());
        verifyNoInteractions(items, bookings, requests);
    }

    @Test
    void preservesServerErrorsAndHandlesConnectionFailure() throws Exception {
        when(requests.getById(1L, 3L)).thenReturn(ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":\"Нет запроса\"}".getBytes(StandardCharsets.UTF_8)));
        mvc.perform(get("/requests/3").header(HEADER, 1)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Нет запроса"));
        when(users.getById(1L)).thenThrow(new ServerUnavailableException(new IllegalStateException("private host")));
        mvc.perform(get("/users/1")).andExpect(status().isBadGateway())
                .andExpect(content().json("{\"error\":\"Сервер ShareIt недоступен\"}"));
        when(users.getById(2L)).thenThrow(new IllegalStateException("private details"));
        mvc.perform(get("/users/2")).andExpect(status().isInternalServerError())
                .andExpect(content().json("{\"error\":\"Внутренняя ошибка сервера\"}"));
    }

    @Test
    void handlesHttpErrorsAndInvalidIds() throws Exception {
        mvc.perform(get("/users/0")).andExpect(status().isBadRequest());
        mvc.perform(get("/items/abc").header(HEADER, 1)).andExpect(status().isBadRequest());
        mvc.perform(get("/requests/-1").header(HEADER, 1)).andExpect(status().isBadRequest());
        mvc.perform(get("/items/search").header(HEADER, 1)).andExpect(status().isBadRequest());
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/users/1")).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/users").contentType(MediaType.TEXT_PLAIN).content("text"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(get("/not-a-route")).andExpect(status().isNotFound());
        verifyNoInteractions(users, items, bookings, requests);
    }

    private ResponseEntity<byte[]> ok(String body) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body.getBytes(StandardCharsets.UTF_8));
    }

    private String bookingJson(LocalDateTime start, LocalDateTime end) {
        return "{\"itemId\":2,\"start\":\"" + start + "\",\"end\":\"" + end + "\"}";
    }
}
