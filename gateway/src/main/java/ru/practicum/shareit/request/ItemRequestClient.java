package ru.practicum.shareit.request;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.practicum.shareit.client.BaseClient;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;

import java.util.Map;

@Service
public class ItemRequestClient extends BaseClient {
    public ItemRequestClient(RestTemplate rest) {
        super(rest);
    }

    public ResponseEntity<byte[]> create(long userId, ItemRequestCreateDto dto) {
        return post("/requests", userId, dto);
    }

    public ResponseEntity<byte[]> getOwn(long userId) {
        return get("/requests", userId, Map.of());
    }

    public ResponseEntity<byte[]> getOthers(long userId) {
        return get("/requests/all", userId, Map.of());
    }

    public ResponseEntity<byte[]> getById(long userId, long id) {
        return get("/requests/" + id, userId, Map.of());
    }
}
