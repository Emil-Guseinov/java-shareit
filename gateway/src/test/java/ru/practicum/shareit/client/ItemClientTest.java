package ru.practicum.shareit.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import ru.practicum.shareit.item.ItemClient;
import ru.practicum.shareit.item.dto.CommentRequestDto;
import ru.practicum.shareit.item.dto.ItemRequestDto;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ItemClientTest extends ClientTestSupport {
    private ItemClient client;

    @BeforeEach
    void setUpClient() {
        client = new ItemClient(rest);
    }

    @Test
    void createUsesExpectedMethodPathAndPayload() throws Exception {
        ItemRequestDto request = new ItemRequestDto("Drill", "Description", true, 9L);
        Object expected = Map.of("id", 4);
        server.expect(requestTo(BASE + "/items")).andExpect(method(HttpMethod.POST))
                .andExpect(header(HEADER, "3"))
                .andExpect(header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(content().json(json(request)))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(json(expected)));
        ResponseEntity<Object> result = client.create(3L, request);
        assertEquals(200, result.getStatusCode().value());
        assertEquals(expected, result.getBody());
    }

    @Test
    void updateUsesExpectedMethodPathAndPayload() throws Exception {
        ItemRequestDto request = new ItemRequestDto(null, null, false);
        Object expected = Map.of("id", 4);
        server.expect(requestTo(BASE + "/items/4")).andExpect(method(HttpMethod.PATCH))
                .andExpect(header(HEADER, "3"))
                .andExpect(header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(content().json(json(request)))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(json(expected)));
        ResponseEntity<Object> result = client.update(3L, 4L, request);
        assertEquals(200, result.getStatusCode().value());
        assertEquals(expected, result.getBody());
    }

    @Test
    void getByIdUsesExpectedMethodPathAndPayload() throws Exception {
        Object expected = Map.of("id", 4);
        server.expect(requestTo(BASE + "/items/4")).andExpect(method(HttpMethod.GET))
                .andExpect(header(HEADER, "3"))
                .andExpect(header(HttpHeaders.ACCEPT, "application/json"))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(json(expected)));
        ResponseEntity<Object> result = client.getById(3L, 4L);
        assertEquals(200, result.getStatusCode().value());
        assertEquals(expected, result.getBody());
    }

    @Test
    void getByOwnerUsesExpectedMethodPathAndPayload() throws Exception {
        Object expected = List.of(Map.of("id", 4));
        server.expect(requestTo(BASE + "/items")).andExpect(method(HttpMethod.GET))
                .andExpect(header(HEADER, "3"))
                .andExpect(header(HttpHeaders.ACCEPT, "application/json"))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(json(expected)));
        ResponseEntity<Object> result = client.getByOwner(3L);
        assertEquals(200, result.getStatusCode().value());
        assertEquals(expected, result.getBody());
    }

    @Test
    void addCommentUsesExpectedMethodPathAndPayload() throws Exception {
        CommentRequestDto request = new CommentRequestDto("Спасибо");
        Object expected = Map.of("id", 4);
        server.expect(requestTo(BASE + "/items/4/comment")).andExpect(method(HttpMethod.POST))
                .andExpect(header(HEADER, "5"))
                .andExpect(header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(content().json(json(request)))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(json(expected)));
        ResponseEntity<Object> result = client.addComment(5L, 4L, request);
        assertEquals(200, result.getStatusCode().value());
        assertEquals(expected, result.getBody());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Дрель + & ? # % _ /", "x&state=ALL", "{text}", "a+b"})
    void searchEncodesTextAsOneQueryValue(String text) throws Exception {
        server.expect(request -> {
                    assertEquals("/items/search", request.getURI().getPath());
                    String query = request.getURI().getRawQuery();
                    assertTrue(query.startsWith("text="));
                    assertFalse(query.substring(5).contains("&"));
                    assertEquals(text, URLDecoder.decode(query.substring(5), StandardCharsets.UTF_8));
                    assertNull(request.getURI().getFragment());
                }).andExpect(method(HttpMethod.GET)).andExpect(header(HEADER, "1"))
                .andRespond(withSuccess(json(List.of()), MediaType.APPLICATION_JSON));
        assertEquals(List.of(), client.search(1L, text).getBody());
    }
}
