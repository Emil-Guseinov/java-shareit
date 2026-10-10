package ru.practicum.shareit.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;
import ru.practicum.shareit.item.ItemClient;
import ru.practicum.shareit.item.dto.ItemRequestDto;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PatchTransportTest {
    @Test
    void jdkTransportSendsPatchHeaderAndJsonBody() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        HttpServer backend = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> header = new AtomicReference<>();
        AtomicReference<String> payload = new AtomicReference<>();
        backend.createContext("/items/2", exchange -> {
            try (exchange) {
                method.set(exchange.getRequestMethod());
                header.set(exchange.getRequestHeaders().getFirst("X-Sharer-User-Id"));
                payload.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                byte[] response = mapper.writeValueAsBytes(Map.of("id", 2, "available", false));
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            }
        });
        backend.start();
        ClientConfiguration configuration = new ClientConfiguration();
        try (HttpClient client = configuration.shareItHttpClient(Duration.ofSeconds(2))) {
            RestTemplate rest = configuration.shareItRestTemplate(new RestTemplateBuilder(), client,
                    "http://127.0.0.1:" + backend.getAddress().getPort(), Duration.ofSeconds(5));
            var result = new ItemClient(rest).update(1L, 2L, new ItemRequestDto(null, null, false));
            assertEquals(200, result.getStatusCode().value());
            assertEquals("PATCH", method.get());
            assertEquals("1", header.get());
            assertFalse(mapper.readTree(payload.get()).get("available").asBoolean());
            assertEquals(Map.of("id", 2, "available", false), result.getBody());
        } finally {
            backend.stop(0);
        }
    }
}
