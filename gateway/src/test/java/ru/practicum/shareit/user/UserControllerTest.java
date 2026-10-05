package ru.practicum.shareit.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MvcResult;
import ru.practicum.shareit.common.exception.ErrorResponse;
import ru.practicum.shareit.common.exception.ServerUnavailableException;
import ru.practicum.shareit.support.ControllerTestSupport;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest extends ControllerTestSupport {
    @MockBean
    private UserClient service;

    @Test
    void createReturnsUser() throws Exception {
        UserDto request = new UserDto(null, "Имя", "name@example.com");
        UserDto expected = new UserDto(1L, "Имя", "name@example.com");
        when(service.create(any(UserDto.class))).thenReturn(ResponseEntity.ok(expected));

        MvcResult result = mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isOk()).andReturn();

        assertBody(result, expected);
        verify(service).create(refEq(request));
    }

    @Test
    void updatePreservesOmittedEmail() throws Exception {
        UserDto request = new UserDto(null, "Новое имя", null);
        UserDto expected = new UserDto(1L, "Новое имя", "name@example.com");
        when(service.update(eq(1L), any(UserDto.class))).thenReturn(ResponseEntity.ok(expected));

        MvcResult result = mvc.perform(patch("/users/1").contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isOk()).andReturn();

        assertBody(result, expected);
        verify(service).update(eq(1L), refEq(request));
    }

    @Test
    void getByIdReturnsUser() throws Exception {
        UserDto expected = new UserDto(1L, "Имя", "name@example.com");
        when(service.getById(1L)).thenReturn(ResponseEntity.ok(expected));

        MvcResult result = mvc.perform(get("/users/1"))
                .andExpect(status().isOk()).andReturn();

        assertBody(result, expected);
        verify(service).getById(1L);
    }

    @Test
    void getAllReturnsUsers() throws Exception {
        UserDto expected = new UserDto(1L, "Имя", "name@example.com");
        when(service.getAll()).thenReturn(ResponseEntity.ok(List.of(expected)));

        MvcResult result = mvc.perform(get("/users"))
                .andExpect(status().isOk()).andReturn();

        assertBody(result, List.of(expected));
        verify(service).getAll();
    }

    @Test
    void deleteReturnsExpectedStatus() throws Exception {
        when(service.remove(1L)).thenReturn(ResponseEntity.noContent().build());
        mvc.perform(delete("/users/1")).andExpect(status().isNoContent());
        verify(service).remove(1L);
    }

    @ParameterizedTest
    @MethodSource("invalidUsers")
    void rejectsInvalidCreate(UserDto request) throws Exception {
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "not-an-email"})
    void rejectsInvalidEmailPatch(String email) throws Exception {
        UserDto request = new UserDto(null, null, email);
        mvc.perform(patch("/users/1").contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsNonPositiveId() throws Exception {
        mvc.perform(get("/users/0")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void reportsUnavailableServerWithoutExposingDetails() throws Exception {
        when(service.getById(1L)).thenThrow(new ServerUnavailableException(new IllegalStateException("private host")));
        MvcResult result = mvc.perform(get("/users/1")).andExpect(status().isBadGateway()).andReturn();
        assertBody(result, new ErrorResponse("Сервер ShareIt недоступен"));
    }

    @Test
    void hidesUnexpectedFailure() throws Exception {
        when(service.getById(1L)).thenThrow(new IllegalStateException("private details"));
        MvcResult result = mvc.perform(get("/users/1")).andExpect(status().isInternalServerError()).andReturn();
        assertBody(result, new ErrorResponse("Внутренняя ошибка сервера"));
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

    static Stream<UserDto> invalidUsers() {
        return Stream.of(new UserDto(), new UserDto(null, "Имя", null),
                new UserDto(null, "", "ok@example.com"), new UserDto(null, "Имя", "bad"),
                new UserDto(null, "Имя", ""));
    }
}
