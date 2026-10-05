package ru.practicum.shareit.client;

import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import ru.practicum.shareit.booking.BookingClient;
import ru.practicum.shareit.booking.BookingState;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.common.exception.ServerUnavailableException;
import ru.practicum.shareit.item.ItemClient;
import ru.practicum.shareit.item.dto.CommentRequestDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.request.ItemRequestClient;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.user.UserClient;
import ru.practicum.shareit.user.dto.UserDto;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GatewayClientTest {
    private static final String BASE = "http://localhost:9090";
    private static final String HEADER = "X-Sharer-User-Id";
    private MockRestServiceServer server;
    private HttpClient httpClient;
    private UserClient users;
    private ItemClient items;
    private BookingClient bookings;
    private ItemRequestClient requests;

    @BeforeEach
    void setUp() {
        ClientConfiguration configuration = new ClientConfiguration();
        httpClient = configuration.shareItHttpClient(Duration.ofSeconds(2));
        RestTemplate rest = configuration.shareItRestTemplate(new RestTemplateBuilder(), httpClient,
                BASE, Duration.ofSeconds(3));
        rest.getMessageConverters().forEach(converter -> {
            if (converter instanceof MappingJackson2HttpMessageConverter json) {
                json.setObjectMapper(Jackson2ObjectMapperBuilder.json()
                        .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build());
            }
        });
        server = MockRestServiceServer.bindTo(rest).build();
        users = new UserClient(rest);
        items = new ItemClient(rest);
        bookings = new BookingClient(rest);
        requests = new ItemRequestClient(rest);
    }

    @AfterEach
    void tearDown() {
        try {
            server.verify();
        } finally {
            httpClient.close();
        }
    }

    @Test
    void userOperationsPreserveMethodsPathsAndBody() {
        server.expect(requestTo(BASE + "/users")).andExpect(method(HttpMethod.POST))
                .andExpect(headerDoesNotExist(HEADER))
                .andExpect(header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(content().json("{\"name\":\"User\",\"email\":\"a@example.com\"}"))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("{\"id\":1}"));
        assertEquals(HttpStatus.CREATED, users.create(new UserDto(null, "User", "a@example.com")).getStatusCode());
        server.verify();
        server.reset();
        server.expect(requestTo(BASE + "/users/1")).andExpect(method(HttpMethod.PATCH))
                .andExpect(content().json("{\"name\":\"New\",\"email\":null}"))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        users.update(1L, new UserDto(null, "New", null));
        server.verify();
        server.reset();
        server.expect(requestTo(BASE + "/users/1")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        users.getById(1L);
        server.verify();
        server.reset();
        server.expect(requestTo(BASE + "/users")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        users.getAll();
        server.verify();
        server.reset();
        server.expect(requestTo(BASE + "/users/1")).andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));
        ResponseEntity<byte[]> deleted = users.remove(1L);
        assertEquals(HttpStatus.NO_CONTENT, deleted.getStatusCode());
        assertFalse(deleted.hasBody());
    }

    @Test
    void itemOperationsForwardHeaderAndOptionalRequestId() {
        server.expect(requestTo(BASE + "/items")).andExpect(method(HttpMethod.POST))
                .andExpect(header(HEADER, "3"))
                .andExpect(content().json("{\"name\":\"Drill\",\"requestId\":9,\"available\":true}"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        items.create(3L, new ItemDto(null, "Drill", "Description", true, 9L));
        server.verify();
        server.reset();
        server.expect(requestTo(BASE + "/items/4")).andExpect(method(HttpMethod.PATCH))
                .andExpect(header(HEADER, "3")).andExpect(content().json("{\"available\":false}"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        items.update(3L, 4L, new ItemDto(null, null, null, false));
        server.verify();
        server.reset();
        server.expect(requestTo(BASE + "/items/4")).andExpect(method(HttpMethod.GET))
                .andExpect(header(HEADER, "3")).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        items.getById(3L, 4L);
        server.verify();
        server.reset();
        server.expect(requestTo(BASE + "/items")).andExpect(method(HttpMethod.GET))
                .andExpect(header(HEADER, "3")).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        items.getByOwner(3L);
        server.verify();
        server.reset();
        server.expect(requestTo(BASE + "/items/4/comment")).andExpect(method(HttpMethod.POST))
                .andExpect(header(HEADER, "5")).andExpect(content().json("{\"text\":\"Спасибо\"}"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        items.addComment(5L, 4L, new CommentRequestDto("Спасибо"));
        server.verify();
        server.reset();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Дрель + & ? # % _ /", "x&state=ALL", "{text}", "a+b"})
    void searchEncodesUserTextAsOneQueryValue(String text) {
        server.expect(request -> {
                    assertEquals("/items/search", request.getURI().getPath());
                    String query = request.getURI().getRawQuery();
                    assertTrue(query.startsWith("text="));
                    assertFalse(query.substring(5).contains("&"));
                    assertEquals(text, URLDecoder.decode(query.substring(5), StandardCharsets.UTF_8));
                    assertNull(request.getURI().getFragment());
                }).andExpect(method(HttpMethod.GET)).andExpect(header(HEADER, "1"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        items.search(1L, text);
        server.verify();
        server.reset();
    }

    @Test
    void bookingOperationsForwardDatesFiltersAndDecision() {
        LocalDateTime start = LocalDateTime.of(2030, 1, 15, 12, 0);
        server.expect(requestTo(BASE + "/bookings")).andExpect(method(HttpMethod.POST))
                .andExpect(header(HEADER, "2"))
                .andExpect(content().json("{\"itemId\":4,\"start\":\"2030-01-15T12:00:00\",\"end\":\"2030-01-16T12:00:00\"}"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        bookings.create(2L, new BookingRequestDto(4L, start, start.plusDays(1)));
        server.verify();
        server.reset();
        server.expect(requestTo(BASE + "/bookings/8?approved=false")).andExpect(method(HttpMethod.PATCH))
                .andExpect(header(HEADER, "3")).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        bookings.approve(3L, 8L, false);
        server.verify();
        server.reset();
        server.expect(requestTo(BASE + "/bookings/8")).andExpect(method(HttpMethod.GET))
                .andExpect(header(HEADER, "2")).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        bookings.getById(2L, 8L);
        server.verify();
        server.reset();
        server.expect(requestTo(BASE + "/bookings?state=PAST")).andExpect(header(HEADER, "2"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        bookings.getByBooker(2L, BookingState.PAST);
        server.verify();
        server.reset();
        server.expect(requestTo(BASE + "/bookings/owner?state=WAITING")).andExpect(header(HEADER, "3"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        bookings.getByOwner(3L, BookingState.WAITING);
        server.verify();
        server.reset();
    }

    @Test
    void requestOperationsForwardAllFourRoutes() {
        server.expect(requestTo(BASE + "/requests")).andExpect(method(HttpMethod.POST))
                .andExpect(header(HEADER, "1")).andExpect(content().json("{\"description\":\"Нужна дрель\"}"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        requests.create(1L, new ItemRequestCreateDto("Нужна дрель"));
        server.verify();
        server.reset();
        server.expect(requestTo(BASE + "/requests")).andExpect(method(HttpMethod.GET))
                .andExpect(header(HEADER, "1")).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        requests.getOwn(1L);
        server.verify();
        server.reset();
        server.expect(requestTo(BASE + "/requests/all")).andExpect(method(HttpMethod.GET))
                .andExpect(header(HEADER, "1")).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        requests.getOthers(1L);
        server.verify();
        server.reset();
        server.expect(requestTo(BASE + "/requests/9")).andExpect(method(HttpMethod.GET))
                .andExpect(header(HEADER, "1")).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        requests.getById(1L, 9L);
        server.verify();
        server.reset();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 403, 404, 409, 500, 503})
    void preservesErrorStatusAndBytesInsteadOfConvertingToSuccess(int status) {
        byte[] body = "{\"error\":\"Ошибка сервера\",\"id\":9223372036854775807}".getBytes(StandardCharsets.UTF_8);
        server.expect(requestTo(BASE + "/users/1"))
                .andRespond(withStatus(HttpStatus.valueOf(status)).body(body).contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.RETRY_AFTER, "5").header(HttpHeaders.ALLOW, "GET")
                        .header(HttpHeaders.CONNECTION, "close").header("X-Internal-Node", "private"));
        ResponseEntity<byte[]> result = users.getById(1L);
        assertEquals(status, result.getStatusCode().value());
        assertArrayEquals(body, result.getBody());
        assertEquals(MediaType.APPLICATION_JSON, result.getHeaders().getContentType());
        assertEquals("5", result.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
        assertEquals("GET", result.getHeaders().getFirst(HttpHeaders.ALLOW));
        assertFalse(result.getHeaders().containsKey(HttpHeaders.CONNECTION));
        assertFalse(result.getHeaders().containsKey("X-Internal-Node"));
    }

    @Test
    void doesNotHideTransportFailuresOrRetryWrite() {
        server.expect(requestTo(BASE + "/users")).andExpect(method(HttpMethod.POST))
                .andRespond(withException(new IOException("Connection refused")));
        ServerUnavailableException failure = assertThrows(ServerUnavailableException.class,
                () -> users.create(new UserDto(null, "User", "a@example.com")));
        assertNotNull(failure.getCause());
    }
}
