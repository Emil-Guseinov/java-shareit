package ru.practicum.shareit.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.shareit.common.exception.ServerUnavailableException;
import ru.practicum.shareit.user.UserClient;
import ru.practicum.shareit.user.UserController;
import ru.practicum.shareit.user.dto.UserDto;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class BaseClientTest extends ClientTestSupport {
    @ParameterizedTest
    @ValueSource(ints = {400, 403, 404, 409, 500, 503})
    void preservesErrorStatusBodyAndPublicHeaders(int status) throws Exception {
        byte[] body = objectMapper.writeValueAsBytes(Map.of("error", "Ошибка сервера", "id", Long.MAX_VALUE));
        server.expect(requestTo(BASE + "/users/1"))
                .andRespond(withStatus(HttpStatus.valueOf(status)).body(body).contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.RETRY_AFTER, "5").header(HttpHeaders.ALLOW, "GET")
                        .header(HttpHeaders.CONNECTION, "close").header("X-Internal-Node", "private"));
        ResponseEntity<Object> result = new UserClient(rest).getById(1L);
        assertEquals(status, result.getStatusCode().value());
        assertArrayEquals(body, assertInstanceOf(byte[].class, result.getBody()));
        assertEquals(MediaType.APPLICATION_JSON, result.getHeaders().getContentType());
        assertEquals("5", result.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
        assertEquals("GET", result.getHeaders().getFirst(HttpHeaders.ALLOW));
        assertFalse(result.getHeaders().containsKey(HttpHeaders.CONNECTION));
        assertFalse(result.getHeaders().containsKey("X-Internal-Node"));
    }

    @Test
    void controllerWritesErrorAsJsonNotBase64() throws Exception {
        Object expected = Map.of("error", "Нет пользователя", "id", Long.MAX_VALUE);
        server.expect(requestTo(BASE + "/users/1"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body(json(expected)));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new UserController(new UserClient(rest))).build();
        var result = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/users/1"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound())
                .andReturn();
        assertEquals(objectMapper.readTree(json(expected)),
                objectMapper.readTree(result.getResponse().getContentAsByteArray()));
    }

    @Test
    void successfulObjectResponseKeepsLargeIdsAndDateStrings() throws Exception {
        Object expected = Map.of("id", Long.MAX_VALUE, "created", "2030-01-15T12:00:00");
        server.expect(requestTo(BASE + "/users/1"))
                .andRespond(withSuccess(json(expected), MediaType.APPLICATION_JSON));
        assertEquals(expected, new UserClient(rest).getById(1L).getBody());
    }

    @Test
    void connectionFailureIsNotRetriedOrReportedAsSuccess() {
        server.expect(requestTo(BASE + "/users")).andExpect(method(HttpMethod.POST))
                .andRespond(withException(new IOException("Connection refused")));
        ServerUnavailableException failure = assertThrows(ServerUnavailableException.class,
                () -> new UserClient(rest).create(new UserDto(null, "User", "a@example.com")));
        assertNotNull(failure.getCause());
    }
}
