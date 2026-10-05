package ru.practicum.shareit.user;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.practicum.shareit.client.BaseClient;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.Map;

@Service
public class UserClient extends BaseClient {
    private static final String API_PREFIX = "/users";

    public UserClient(RestTemplate rest) {
        super(rest);
    }

    public ResponseEntity<Object> create(UserDto dto) {
        return post(API_PREFIX, null, dto);
    }

    public ResponseEntity<Object> update(long id, UserDto dto) {
        return patch(API_PREFIX + "/" + id, null, Map.of(), dto);
    }

    public ResponseEntity<Object> getById(long id) {
        return get(API_PREFIX + "/" + id, null, Map.of());
    }

    public ResponseEntity<Object> getAll() {
        return get(API_PREFIX, null, Map.of());
    }

    public ResponseEntity<Object> remove(long id) {
        return delete(API_PREFIX + "/" + id);
    }
}
