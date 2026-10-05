package ru.practicum.shareit.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import ru.practicum.shareit.booking.BookingClient;
import ru.practicum.shareit.booking.BookingState;
import ru.practicum.shareit.booking.dto.BookingRequestDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class BookingClientTest extends ClientTestSupport {
    private BookingClient client;

    @BeforeEach
    void setUpClient() {
        client = new BookingClient(rest);
    }

    @Test
    void createUsesExpectedMethodPathAndPayload() throws Exception {
        BookingRequestDto request = new BookingRequestDto(4L, LocalDateTime.of(2030, 1, 15, 12, 0), LocalDateTime.of(2030, 1, 16, 12, 0));
        Object expected = Map.of("id", 4);
        server.expect(requestTo(BASE + "/bookings")).andExpect(method(HttpMethod.POST))
                .andExpect(header(HEADER, "2"))
                .andExpect(header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(content().json(json(request)))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(json(expected)));
        ResponseEntity<Object> result = client.create(2L, request);
        assertEquals(200, result.getStatusCode().value());
        assertEquals(expected, result.getBody());
    }

    @Test
    void approveUsesExpectedMethodPathAndPayload() throws Exception {
        Object expected = Map.of("id", 4);
        server.expect(requestTo(BASE + "/bookings/8?approved=false")).andExpect(method(HttpMethod.PATCH))
                .andExpect(header(HEADER, "3"))
                .andExpect(header(HttpHeaders.ACCEPT, "application/json"))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(json(expected)));
        ResponseEntity<Object> result = client.approve(3L, 8L, false);
        assertEquals(200, result.getStatusCode().value());
        assertEquals(expected, result.getBody());
    }

    @Test
    void getByIdUsesExpectedMethodPathAndPayload() throws Exception {
        Object expected = Map.of("id", 4);
        server.expect(requestTo(BASE + "/bookings/8")).andExpect(method(HttpMethod.GET))
                .andExpect(header(HEADER, "2"))
                .andExpect(header(HttpHeaders.ACCEPT, "application/json"))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(json(expected)));
        ResponseEntity<Object> result = client.getById(2L, 8L);
        assertEquals(200, result.getStatusCode().value());
        assertEquals(expected, result.getBody());
    }

    @Test
    void getByBookerUsesExpectedMethodPathAndPayload() throws Exception {
        Object expected = List.of(Map.of("id", 4));
        server.expect(requestTo(BASE + "/bookings?state=PAST")).andExpect(method(HttpMethod.GET))
                .andExpect(header(HEADER, "2"))
                .andExpect(header(HttpHeaders.ACCEPT, "application/json"))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(json(expected)));
        ResponseEntity<Object> result = client.getByBooker(2L, BookingState.PAST);
        assertEquals(200, result.getStatusCode().value());
        assertEquals(expected, result.getBody());
    }

    @Test
    void getByOwnerUsesExpectedMethodPathAndPayload() throws Exception {
        Object expected = List.of(Map.of("id", 4));
        server.expect(requestTo(BASE + "/bookings/owner?state=WAITING")).andExpect(method(HttpMethod.GET))
                .andExpect(header(HEADER, "3"))
                .andExpect(header(HttpHeaders.ACCEPT, "application/json"))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(json(expected)));
        ResponseEntity<Object> result = client.getByOwner(3L, BookingState.WAITING);
        assertEquals(200, result.getStatusCode().value());
        assertEquals(expected, result.getBody());
    }
}
