package ru.practicum.shareit.user;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.practicum.shareit.client.BaseClient;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.Map;

@Service
public class UserClient extends BaseClient {
    public UserClient(RestTemplate rest) {
        super(rest);
    }

    public ResponseEntity<byte[]> create(UserDto dto) {
        return post("/users", null, dto);
    }

    public ResponseEntity<byte[]> update(long id, UserDto dto) {
        return patch("/users/" + id, null, Map.of(), dto);
    }

    public ResponseEntity<byte[]> getById(long id) {
        return get("/users/" + id, null, Map.of());
    }

    public ResponseEntity<byte[]> getAll() {
        return get("/users", null, Map.of());
    }

    public ResponseEntity<byte[]> remove(long id) {
        return delete("/users/" + id);
    }
}
