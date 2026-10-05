package ru.practicum.shareit.client;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;
import ru.practicum.shareit.item.ItemClient;
import ru.practicum.shareit.item.dto.ItemDto;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatchTransportTest {
    @Test
    void jdkTransportSendsPatchHeaderAndJsonBody() throws Exception {
        HttpServer backend = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> header = new AtomicReference<>();
        AtomicReference<String> payload = new AtomicReference<>();
        backend.createContext("/items/2", exchange -> {
            try (exchange) {
                method.set(exchange.getRequestMethod());
                header.set(exchange.getRequestHeaders().getFirst("X-Sharer-User-Id"));
                payload.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                byte[] response = "{\"id\":2,\"available\":false}".getBytes(StandardCharsets.UTF_8);
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
            var result = new ItemClient(rest).update(1L, 2L, new ItemDto(null, null, null, false));
            assertEquals(200, result.getStatusCode().value());
            assertEquals("PATCH", method.get());
            assertEquals("1", header.get());
            assertTrue(payload.get().contains("\"available\":false"));
        } finally {
            backend.stop(0);
        }
    }
}
