package ru.practicum.shareit.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import ru.practicum.shareit.request.ItemRequestClient;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ItemRequestClientTest extends ClientTestSupport {
    private ItemRequestClient client;

    @BeforeEach
    void setUpClient() {
        client = new ItemRequestClient(rest);
    }

    @Test
    void createUsesExpectedMethodPathAndPayload() throws Exception {
        ItemRequestCreateDto request = new ItemRequestCreateDto("Нужна дрель");
        Object expected = Map.of("id", 4);
        server.expect(requestTo(BASE + "/requests")).andExpect(method(HttpMethod.POST))
                .andExpect(header(HEADER, "1"))
                .andExpect(header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(content().json(json(request)))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(json(expected)));
        ResponseEntity<Object> result = client.create(1L, request);
        assertEquals(200, result.getStatusCode().value());
        assertEquals(expected, result.getBody());
    }

    @Test
    void getOwnUsesExpectedMethodPathAndPayload() throws Exception {
        Object expected = List.of(Map.of("id", 4));
        server.expect(requestTo(BASE + "/requests")).andExpect(method(HttpMethod.GET))
                .andExpect(header(HEADER, "1"))
                .andExpect(header(HttpHeaders.ACCEPT, "application/json"))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(json(expected)));
        ResponseEntity<Object> result = client.getOwn(1L);
        assertEquals(200, result.getStatusCode().value());
        assertEquals(expected, result.getBody());
    }

    @Test
    void getOthersUsesExpectedMethodPathAndPayload() throws Exception {
        Object expected = List.of(Map.of("id", 4));
        server.expect(requestTo(BASE + "/requests/all")).andExpect(method(HttpMethod.GET))
                .andExpect(header(HEADER, "1"))
                .andExpect(header(HttpHeaders.ACCEPT, "application/json"))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(json(expected)));
        ResponseEntity<Object> result = client.getOthers(1L);
        assertEquals(200, result.getStatusCode().value());
        assertEquals(expected, result.getBody());
    }

    @Test
    void getByIdUsesExpectedMethodPathAndPayload() throws Exception {
        Object expected = Map.of("id", 4);
        server.expect(requestTo(BASE + "/requests/9")).andExpect(method(HttpMethod.GET))
                .andExpect(header(HEADER, "1"))
                .andExpect(header(HttpHeaders.ACCEPT, "application/json"))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(json(expected)));
        ResponseEntity<Object> result = client.getById(1L, 9L);
        assertEquals(200, result.getStatusCode().value());
        assertEquals(expected, result.getBody());
    }
}
