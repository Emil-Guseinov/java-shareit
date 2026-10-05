package ru.practicum.shareit.client;

import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import ru.practicum.shareit.common.exception.ServerUnavailableException;

import java.util.List;
import java.util.Map;

public abstract class BaseClient {
    private final RestTemplate rest;

    protected BaseClient(RestTemplate rest) {
        this.rest = rest;
    }

    protected ResponseEntity<byte[]> get(String path, Long userId, Map<String, ?> parameters) {
        return send(HttpMethod.GET, path, userId, parameters, null);
    }

    protected ResponseEntity<byte[]> post(String path, Long userId, Object body) {
        return send(HttpMethod.POST, path, userId, Map.of(), body);
    }

    protected ResponseEntity<byte[]> patch(String path, Long userId, Map<String, ?> parameters, Object body) {
        return send(HttpMethod.PATCH, path, userId, parameters, body);
    }

    protected ResponseEntity<byte[]> delete(String path) {
        return send(HttpMethod.DELETE, path, null, Map.of(), null);
    }

    private ResponseEntity<byte[]> send(HttpMethod method, String path, Long userId,
                                        Map<String, ?> parameters, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        if (userId != null) {
            headers.set("X-Sharer-User-Id", userId.toString());
        }
        try {
            ResponseEntity<byte[]> result = rest.exchange(path, method,
                    new HttpEntity<>(body, headers), byte[].class, parameters);
            return response(result.getStatusCode(), result.getHeaders(), result.getBody());
        } catch (RestClientResponseException exception) {
            return response(exception.getStatusCode(), exception.getResponseHeaders(),
                    exception.getResponseBodyAsByteArray());
        } catch (RestClientException exception) {
            throw new ServerUnavailableException(exception);
        }
    }

    private ResponseEntity<byte[]> response(HttpStatusCode status, HttpHeaders source, byte[] body) {
        HttpHeaders headers = new HttpHeaders();
        if (source != null) {
            for (String name : List.of(HttpHeaders.CONTENT_TYPE, HttpHeaders.ALLOW, HttpHeaders.RETRY_AFTER)) {
                List<String> values = source.get(name);
                if (values != null) {
                    headers.put(name, List.copyOf(values));
                }
            }
        }
        // Передаём JSON без повторного разбора: не меняем даты, числа и тело ошибки сервера.
        return new ResponseEntity<>(body, headers, status);
    }
}
