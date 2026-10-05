package ru.practicum.shareit.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.BookingController;
import ru.practicum.shareit.booking.BookingState;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookerDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingItemDto;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.common.exception.BadRequestException;
import ru.practicum.shareit.common.exception.ConflictException;
import ru.practicum.shareit.common.exception.ForbiddenException;
import ru.practicum.shareit.common.exception.NotFoundException;
import ru.practicum.shareit.item.ItemController;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemResponseDto;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.request.ItemRequestController;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.RequestItemDto;
import ru.practicum.shareit.request.service.ItemRequestService;
import ru.practicum.shareit.user.UserController;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({UserController.class, ItemController.class, BookingController.class, ItemRequestController.class})
class ServerControllerTest {
    private static final String HEADER = "X-Sharer-User-Id";
    private static final LocalDateTime DATE = LocalDateTime.of(2030, 1, 15, 12, 0);
    @Autowired
    private MockMvc mvc;
    @MockBean
    private UserService users;
    @MockBean
    private ItemService items;
    @MockBean
    private BookingService bookings;
    @MockBean
    private ItemRequestService requests;

    @Test
    void userCrud() throws Exception {
        UserDto user = new UserDto(1L, "Имя", "name@example.com");
        when(users.create(any())).thenReturn(user);
        when(users.update(eq(1L), any())).thenReturn(user);
        when(users.getById(1L)).thenReturn(user);
        when(users.getAll()).thenReturn(List.of(user));
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Имя\",\"email\":\"name@example.com\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
        mvc.perform(patch("/users/1").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Имя\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Имя"));
        mvc.perform(get("/users/1")).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(user.getEmail()));
        mvc.perform(get("/users")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1));
        mvc.perform(delete("/users/1")).andExpect(status().isOk());
        verify(users).delete(1L);
    }

    @Test
    void itemRoutesAndComment() throws Exception {
        ItemDto item = new ItemDto(2L, "Дрель", "Описание", true, 3L);
        when(items.create(eq(1L), any())).thenReturn(item);
        when(items.update(eq(1L), eq(2L), any())).thenReturn(item);
        when(items.search("Дрель")).thenReturn(List.of(item));
        ItemResponseDto response = new ItemResponseDto(2L, "Дрель", "Описание", true, null, null, List.of(), 3L);
        when(items.getById(1L, 2L)).thenReturn(response);
        when(items.getByOwner(1L)).thenReturn(List.of(response));
        when(items.addComment(eq(1L), eq(2L), any())).thenReturn(new CommentDto(4L, "Отзыв", "Имя", DATE));
        mvc.perform(post("/items").header(HEADER, 1).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Дрель\",\"description\":\"Описание\",\"available\":true,\"requestId\":3}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.requestId").value(3));
        mvc.perform(patch("/items/2").header(HEADER, 1).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"available\":false}"))
                .andExpect(status().isOk());
        mvc.perform(get("/items/2").header(HEADER, 1)).andExpect(status().isOk())
                .andExpect(jsonPath("$.comments").isArray());
        mvc.perform(get("/items").header(HEADER, 1)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));
        mvc.perform(get("/items/search").header(HEADER, 1).param("text", "Дрель"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Дрель"));
        mvc.perform(post("/items/2/comment").header(HEADER, 1).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Отзыв\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.authorName").value("Имя"));
    }

    @Test
    void bookingRoutes() throws Exception {
        BookingDto booking = new BookingDto(5L, DATE, DATE.plusDays(1), BookingStatus.WAITING,
                new BookingItemDto(2L, "Дрель"), new BookerDto(1L));
        when(bookings.create(eq(1L), any())).thenReturn(booking);
        when(bookings.approve(1L, 5L, true)).thenReturn(booking);
        when(bookings.getById(1L, 5L)).thenReturn(booking);
        when(bookings.getByBooker(1L, BookingState.ALL)).thenReturn(List.of(booking));
        when(bookings.getByOwner(1L, BookingState.PAST)).thenReturn(List.of(booking));
        mvc.perform(post("/bookings").header(HEADER, 1).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":2,\"start\":\"2030-01-15T12:00:00\",\"end\":\"2030-01-16T12:00:00\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.booker.id").value(1));
        mvc.perform(patch("/bookings/5").header(HEADER, 1).param("approved", "true"))
                .andExpect(status().isOk());
        mvc.perform(get("/bookings/5").header(HEADER, 1)).andExpect(status().isOk())
                .andExpect(jsonPath("$.start").value("2030-01-15T12:00:00"));
        mvc.perform(get("/bookings").header(HEADER, 1)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("WAITING"));
        mvc.perform(get("/bookings/owner").header(HEADER, 1).param("state", "PAST"))
                .andExpect(status().isOk());
    }

    @Test
    void requestRoutes() throws Exception {
        ItemRequestDto dto = new ItemRequestDto(3L, "Нужна дрель", DATE,
                List.of(new RequestItemDto(2L, "Дрель", 8L)));
        when(requests.create(eq(1L), any())).thenReturn(dto);
        when(requests.getOwn(1L)).thenReturn(List.of(dto));
        when(requests.getOthers(1L)).thenReturn(List.of(dto));
        when(requests.getById(1L, 3L)).thenReturn(dto);
        mvc.perform(post("/requests").header(HEADER, 1).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Нужна дрель\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.description").value("Нужна дрель"));
        mvc.perform(get("/requests").header(HEADER, 1)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].items[0].ownerId").value(8));
        mvc.perform(get("/requests/all").header(HEADER, 1)).andExpect(status().isOk());
        mvc.perform(get("/requests/3").header(HEADER, 1)).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(2));
    }

    @Test
    void translatesDomainExceptions() throws Exception {
        when(users.getById(1L)).thenThrow(new NotFoundException("Не найден"));
        mvc.perform(get("/users/1")).andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("Не найден"));
        doThrow(new ConflictException("Конфликт")).when(users).delete(1L);
        mvc.perform(delete("/users/1")).andExpect(status().isConflict());
        when(items.getById(1L, 2L)).thenThrow(new ForbiddenException("Нет доступа"));
        mvc.perform(get("/items/2").header(HEADER, 1)).andExpect(status().isForbidden());
        when(bookings.approve(1L, 2L, true)).thenThrow(new BadRequestException("Неверное состояние"));
        mvc.perform(patch("/bookings/2").header(HEADER, 1).param("approved", "true"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/items", "/bookings", "/requests"})
    void requiresUserHeader(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isBadRequest());
    }

    @Test
    void handlesMalformedRequestsWithoutTurningThemInto500() throws Exception {
        mvc.perform(get("/users/not-a-number")).andExpect(status().isBadRequest());
        mvc.perform(get("/items/search").header(HEADER, 1)).andExpect(status().isBadRequest());
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/users/1")).andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/not-a-route")).andExpect(status().isNotFound());
        mvc.perform(post("/users").contentType(MediaType.TEXT_PLAIN).content("abc"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void hidesUnexpectedErrorAndDatabaseDetails() throws Exception {
        when(users.getById(1L)).thenThrow(new IllegalStateException("secret SQL"));
        mvc.perform(get("/users/1")).andExpect(status().isInternalServerError())
                .andExpect(content().json("{\"error\":\"Внутренняя ошибка сервера\"}"));
        when(users.getById(2L)).thenThrow(new org.springframework.dao.DataIntegrityViolationException("secret SQL"));
        mvc.perform(get("/users/2")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Данные конфликтуют с существующими записями"));
    }
}
