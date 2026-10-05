package ru.practicum.shareit.request;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.practicum.shareit.client.BaseClient;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;

import java.util.Map;

@Service
public class ItemRequestClient extends BaseClient {
    private static final String API_PREFIX = "/requests";

    public ItemRequestClient(RestTemplate rest) {
        super(rest);
    }

    public ResponseEntity<Object> create(long userId, ItemRequestCreateDto dto) {
        return post(API_PREFIX, userId, dto);
    }

    public ResponseEntity<Object> getOwn(long userId) {
        return get(API_PREFIX, userId, Map.of());
    }

    public ResponseEntity<Object> getOthers(long userId) {
        return get(API_PREFIX + "/all", userId, Map.of());
    }

    public ResponseEntity<Object> getById(long userId, long id) {
        return get(API_PREFIX + "/" + id, userId, Map.of());
    }
}
