package ru.practicum.shareit.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.practicum.shareit.common.exception.ConflictException;
import ru.practicum.shareit.common.exception.NotFoundException;
import ru.practicum.shareit.support.AbstractIntegrationTest;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceTest extends AbstractIntegrationTest {
    @Autowired
    private UserService service;

    @Test
    void shouldCreateUserAndIgnoreClientId() {
        UserDto result = service.create(new UserDto(Long.MAX_VALUE, "Эмиль", "emil@example.com"));
        assertNotNull(result.getId());
        assertNotEquals(Long.MAX_VALUE, result.getId().longValue());
        entityManager.clear();
        assertEquals("Эмиль", service.getById(result.getId()).getName());
        assertEquals("emil@example.com", service.getById(result.getId()).getEmail());
    }

    @Test
    void shouldRejectDuplicateEmail() {
        User existing = user("Первый");
        assertThrows(ConflictException.class,
                () -> service.create(new UserDto(null, "Второй", existing.getEmail())));
    }

    @Test
    void shouldKeepMissingFieldsOnPatch() {
        User existing = user("Первый");
        UserDto updated = service.update(existing.getId(), new UserDto(null, "Новое имя", null));
        assertEquals("Новое имя", updated.getName());
        assertEquals(existing.getEmail(), updated.getEmail());
    }

    @Test
    void shouldChangeEmailWithoutChangingName() {
        User existing = user("Первый");
        UserDto updated = service.update(existing.getId(), new UserDto(null, null, "new@example.com"));
        assertEquals("Первый", updated.getName());
        assertEquals("new@example.com", updated.getEmail());
        entityManager.clear();
        assertEquals("new@example.com", service.getById(existing.getId()).getEmail());
    }

    @Test
    void shouldAllowUnchangedEmail() {
        User existing = user("Первый");
        UserDto result = service.update(existing.getId(), new UserDto(null, null, existing.getEmail()));
        assertEquals(existing.getEmail(), result.getEmail());
    }

    @Test
    void shouldRejectEmailBelongingToAnotherUser() {
        User first = user("Первый");
        User second = user("Второй");
        assertThrows(ConflictException.class,
                () -> service.update(first.getId(), new UserDto(null, null, second.getEmail())));
    }

    @Test
    void shouldListUsersByIdAndDeleteUser() {
        User first = user("Первый");
        User second = user("Второй");
        List<Long> ids = service.getAll().stream().map(UserDto::getId).toList();
        assertTrue(ids.indexOf(first.getId()) < ids.indexOf(second.getId()));
        service.delete(first.getId());
        entityManager.clear();
        assertFalse(users.existsById(first.getId()));
        assertTrue(users.existsById(second.getId()));
    }

    @Test
    void shouldRejectUnknownUserOnRead() {
        assertThrows(NotFoundException.class, () -> service.getById(Long.MAX_VALUE));
    }

    @Test
    void shouldRejectUnknownUserOnUpdate() {
        assertThrows(NotFoundException.class,
                () -> service.update(Long.MAX_VALUE, new UserDto(null, "Имя", null)));
    }

    @Test
    void shouldRejectUnknownUserOnDelete() {
        assertThrows(NotFoundException.class, () -> service.delete(Long.MAX_VALUE));
    }
}
