package ru.practicum.shareit.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import ru.practicum.shareit.user.UserClient;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class UserClientTest extends ClientTestSupport {
    private UserClient client;

    @BeforeEach
    void setUpClient() {
        client = new UserClient(rest);
    }

    @Test
    void createUsesExpectedMethodPathAndPayload() throws Exception {
        UserDto request = new UserDto(null, "User", "a@example.com");
        Object expected = Map.of("id", 4);
        server.expect(requestTo(BASE + "/users")).andExpect(method(HttpMethod.POST))
                .andExpect(headerDoesNotExist(HEADER))
                .andExpect(header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(content().json(json(request)))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(json(expected)));
        ResponseEntity<Object> result = client.create(request);
        assertEquals(201, result.getStatusCode().value());
        assertEquals(expected, result.getBody());
    }

    @Test
    void updateUsesExpectedMethodPathAndPayload() throws Exception {
        UserDto request = new UserDto(null, "New", null);
        Object expected = Map.of("id", 4);
        server.expect(requestTo(BASE + "/users/1")).andExpect(method(HttpMethod.PATCH))
                .andExpect(headerDoesNotExist(HEADER))
                .andExpect(header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(content().json(json(request)))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(json(expected)));
        ResponseEntity<Object> result = client.update(1L, request);
        assertEquals(200, result.getStatusCode().value());
        assertEquals(expected, result.getBody());
    }

    @Test
    void getByIdUsesExpectedMethodPathAndPayload() throws Exception {
        Object expected = Map.of("id", 4);
        server.expect(requestTo(BASE + "/users/1")).andExpect(method(HttpMethod.GET))
                .andExpect(headerDoesNotExist(HEADER))
                .andExpect(header(HttpHeaders.ACCEPT, "application/json"))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(json(expected)));
        ResponseEntity<Object> result = client.getById(1L);
        assertEquals(200, result.getStatusCode().value());
        assertEquals(expected, result.getBody());
    }

    @Test
    void getAllUsesExpectedMethodPathAndPayload() throws Exception {
        Object expected = List.of(Map.of("id", 4));
        server.expect(requestTo(BASE + "/users")).andExpect(method(HttpMethod.GET))
                .andExpect(headerDoesNotExist(HEADER))
                .andExpect(header(HttpHeaders.ACCEPT, "application/json"))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(json(expected)));
        ResponseEntity<Object> result = client.getAll();
        assertEquals(200, result.getStatusCode().value());
        assertEquals(expected, result.getBody());
    }

    @Test
    void removeUsesExpectedMethodPathAndPayload() throws Exception {
        server.expect(requestTo(BASE + "/users/1")).andExpect(method(HttpMethod.DELETE))
                .andExpect(headerDoesNotExist(HEADER))
                .andExpect(header(HttpHeaders.ACCEPT, "application/json"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));
        ResponseEntity<Object> result = client.remove(1L);
        assertEquals(204, result.getStatusCode().value());
        assertFalse(result.hasBody());
    }
}
