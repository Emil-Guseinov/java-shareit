package ru.practicum.shareit.item;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.practicum.shareit.client.BaseClient;
import ru.practicum.shareit.item.dto.CommentRequestDto;
import ru.practicum.shareit.item.dto.ItemDto;

import java.util.Map;

@Service
public class ItemClient extends BaseClient {
    public ItemClient(RestTemplate rest) {
        super(rest);
    }

    public ResponseEntity<byte[]> create(long userId, ItemDto dto) {
        return post("/items", userId, dto);
    }

    public ResponseEntity<byte[]> update(long userId, long id, ItemDto dto) {
        return patch("/items/" + id, userId, Map.of(), dto);
    }

    public ResponseEntity<byte[]> getById(long userId, long id) {
        return get("/items/" + id, userId, Map.of());
    }

    public ResponseEntity<byte[]> getByOwner(long userId) {
        return get("/items", userId, Map.of());
    }

    public ResponseEntity<byte[]> search(long userId, String text) {
        return get("/items/search?text={text}", userId, Map.of("text", text));
    }

    public ResponseEntity<byte[]> addComment(long userId, long id, CommentRequestDto dto) {
        return post("/items/" + id + "/comment", userId, dto);
    }
}
