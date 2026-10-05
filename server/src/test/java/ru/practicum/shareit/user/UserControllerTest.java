package ru.practicum.shareit.user;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import ru.practicum.shareit.common.exception.ConflictException;
import ru.practicum.shareit.common.exception.ErrorResponse;
import ru.practicum.shareit.common.exception.NotFoundException;
import ru.practicum.shareit.support.ControllerTestSupport;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest extends ControllerTestSupport {
    @MockBean
    private UserService service;

    @Test
    void createReturnsUser() throws Exception {
        UserDto request = new UserDto(null, "Имя", "name@example.com");
        UserDto expected = new UserDto(1L, "Имя", "name@example.com");
        when(service.create(any(UserDto.class))).thenReturn(expected);

        MvcResult result = mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isOk()).andReturn();

        assertBody(result, expected);
        verify(service).create(refEq(request));
    }

    @Test
    void updatePreservesOmittedEmail() throws Exception {
        UserDto request = new UserDto(null, "Новое имя", null);
        UserDto expected = new UserDto(1L, "Новое имя", "name@example.com");
        when(service.update(eq(1L), any(UserDto.class))).thenReturn(expected);

        MvcResult result = mvc.perform(patch("/users/1").contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isOk()).andReturn();

        assertBody(result, expected);
        verify(service).update(eq(1L), refEq(request));
    }

    @Test
    void getByIdReturnsUser() throws Exception {
        UserDto expected = new UserDto(1L, "Имя", "name@example.com");
        when(service.getById(1L)).thenReturn(expected);

        MvcResult result = mvc.perform(get("/users/1"))
                .andExpect(status().isOk()).andReturn();

        assertBody(result, expected);
        verify(service).getById(1L);
    }

    @Test
    void getAllReturnsUsers() throws Exception {
        UserDto expected = new UserDto(1L, "Имя", "name@example.com");
        when(service.getAll()).thenReturn(List.of(expected));

        MvcResult result = mvc.perform(get("/users"))
                .andExpect(status().isOk()).andReturn();

        assertBody(result, List.of(expected));
        verify(service).getAll();
    }

    @Test
    void deleteReturnsExpectedStatus() throws Exception {
        mvc.perform(delete("/users/1")).andExpect(status().isOk());
        verify(service).delete(1L);
    }

    @Test
    void returnsNotFound() throws Exception {
        when(service.getById(1L)).thenThrow(new NotFoundException("Не найден"));
        MvcResult result = mvc.perform(get("/users/1")).andExpect(status().isNotFound()).andReturn();
        assertBody(result, new ErrorResponse("Не найден"));
    }

    @Test
    void returnsConflictOnDelete() throws Exception {
        doThrow(new ConflictException("Конфликт")).when(service).delete(1L);
        MvcResult result = mvc.perform(delete("/users/1")).andExpect(status().isConflict()).andReturn();
        assertBody(result, new ErrorResponse("Конфликт"));
    }

    @Test
    void hidesUnexpectedFailure() throws Exception {
        when(service.getById(1L)).thenThrow(new IllegalStateException("secret SQL"));
        MvcResult result = mvc.perform(get("/users/1")).andExpect(status().isInternalServerError()).andReturn();
        assertBody(result, new ErrorResponse("Внутренняя ошибка сервера"));
    }

    @Test
    void hidesDatabaseDetails() throws Exception {
        when(service.getById(1L)).thenThrow(new org.springframework.dao.DataIntegrityViolationException("secret SQL"));
        MvcResult result = mvc.perform(get("/users/1")).andExpect(status().isConflict()).andReturn();
        assertBody(result, new ErrorResponse("Данные конфликтуют с существующими записями"));
    }

    @Test
    void rejectsNonNumericId() throws Exception {
        mvc.perform(get("/users/not-a-number")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsUnsupportedMethod() throws Exception {
        mvc.perform(put("/users/1")).andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsUnsupportedContentType() throws Exception {
        mvc.perform(post("/users").contentType(MediaType.TEXT_PLAIN).content("text"))
                .andExpect(status().isUnsupportedMediaType());
        verifyNoInteractions(service);
    }

    @Test
    void unknownRouteRemainsNotFound() throws Exception {
        mvc.perform(get("/not-a-route")).andExpect(status().isNotFound());
        verifyNoInteractions(service);
    }
}
