package ru.practicum.shareit.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.shareit.common.exception.ConflictException;
import ru.practicum.shareit.common.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;
import ru.practicum.shareit.user.service.UserServiceImpl;
import ru.practicum.shareit.user.storage.InMemoryUserStorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserServiceTest {
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(new InMemoryUserStorage());
    }

    @Test
    void shouldCreateAndGetUser() {
        UserDto created = userService.create(new UserDto(null, "Alex", "alex@example.com"));
        UserDto loaded = userService.getById(created.getId());

        assertEquals(1L, created.getId());
        assertEquals(created.getId(), loaded.getId());
        assertEquals("Alex", loaded.getName());
        assertEquals("alex@example.com", loaded.getEmail());
    }

    @Test
    void shouldUpdateOnlyProvidedFields() {
        UserDto created = userService.create(new UserDto(null, "Alex", "alex@example.com"));
        UserDto patch = new UserDto();

        patch.setName("Alexander");

        UserDto updated = userService.update(created.getId(), patch);

        assertEquals("Alexander", updated.getName());
        assertEquals("alex@example.com", updated.getEmail());
    }

    @Test
    void shouldRejectDuplicateEmail() {
        userService.create(new UserDto(null, "Alex", "alex@example.com"));

        assertThrows(ConflictException.class,
                () -> userService.create(new UserDto(null, "Bob", "alex@example.com")));
    }

    @Test
    void shouldRejectDuplicateEmailOnUpdate() {
        userService.create(new UserDto(null, "Alex", "alex@example.com"));
        UserDto bob = userService.create(new UserDto(null, "Bob", "bob@example.com"));
        UserDto patch = new UserDto();

        patch.setEmail("alex@example.com");

        assertThrows(ConflictException.class, () -> userService.update(bob.getId(), patch));
    }

    @Test
    void shouldDeleteUser() {
        UserDto created = userService.create(new UserDto(null, "Alex", "alex@example.com"));

        userService.delete(created.getId());

        assertThrows(NotFoundException.class, () -> userService.getById(created.getId()));
    }
}
