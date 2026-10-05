package ru.practicum.shareit.item;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.practicum.shareit.client.BaseClient;
import ru.practicum.shareit.item.dto.CommentRequestDto;
import ru.practicum.shareit.item.dto.ItemRequestDto;

import java.util.Map;

@Service
public class ItemClient extends BaseClient {
    private static final String API_PREFIX = "/items";

    public ItemClient(RestTemplate rest) {
        super(rest);
    }

    public ResponseEntity<Object> create(long userId, ItemRequestDto dto) {
        return post(API_PREFIX, userId, dto);
    }

    public ResponseEntity<Object> update(long userId, long id, ItemRequestDto dto) {
        return patch(API_PREFIX + "/" + id, userId, Map.of(), dto);
    }

    public ResponseEntity<Object> getById(long userId, long id) {
        return get(API_PREFIX + "/" + id, userId, Map.of());
    }

    public ResponseEntity<Object> getByOwner(long userId) {
        return get(API_PREFIX, userId, Map.of());
    }

    public ResponseEntity<Object> search(long userId, String text) {
        return get(API_PREFIX + "/search?text={text}", userId, Map.of("text", text));
    }

    public ResponseEntity<Object> addComment(long userId, long id, CommentRequestDto dto) {
        return post(API_PREFIX + "/" + id + "/comment", userId, dto);
    }
}
