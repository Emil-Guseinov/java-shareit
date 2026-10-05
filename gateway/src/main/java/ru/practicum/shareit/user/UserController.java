package ru.practicum.shareit.user;

import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.validation.Create;
import ru.practicum.shareit.validation.Update;

@RestController
@Validated
@RequestMapping(path = "/users")
public class UserController {
    private final UserClient userClient;

    public UserController(UserClient userClient) {
        this.userClient = userClient;
    }

    @PostMapping
    public ResponseEntity<byte[]> create(@Validated(Create.class) @RequestBody UserDto userDto) {
        return userClient.create(userDto);
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<byte[]> update(@PathVariable @Positive long userId,
                                         @Validated(Update.class) @RequestBody UserDto userDto) {
        return userClient.update(userId, userDto);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<byte[]> getById(@PathVariable @Positive long userId) {
        return userClient.getById(userId);
    }

    @GetMapping
    public ResponseEntity<byte[]> getAll() {
        return userClient.getAll();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<byte[]> delete(@PathVariable @Positive long userId) {
        return userClient.remove(userId);
    }
}
