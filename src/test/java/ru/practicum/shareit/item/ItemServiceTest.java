package ru.practicum.shareit.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.shareit.common.exception.ForbiddenException;
import ru.practicum.shareit.common.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.item.service.ItemServiceImpl;
import ru.practicum.shareit.item.storage.InMemoryItemStorage;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;
import ru.practicum.shareit.user.service.UserServiceImpl;
import ru.practicum.shareit.user.storage.InMemoryUserStorage;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemServiceTest {
    private InMemoryUserStorage userStorage;
    private UserService userService;
    private ItemService itemService;

    @BeforeEach
    void setUp() {
        userStorage = new InMemoryUserStorage();
        userService = new UserServiceImpl(userStorage);
        itemService = new ItemServiceImpl(new InMemoryItemStorage(), userStorage);
    }

    @Test
    void shouldCreateItemForExistingOwner() {
        UserDto owner = createUser("owner@example.com");

        ItemDto created = itemService.create(owner.getId(),
                new ItemDto(null, "Дрель", "Ударная дрель", true));

        assertEquals(1L, created.getId());
        assertEquals("Дрель", created.getName());
        assertTrue(created.getAvailable());
    }

    @Test
    void shouldRejectItemForUnknownOwner() {
        ItemDto item = new ItemDto(null, "Дрель", "Ударная дрель", true);

        assertThrows(NotFoundException.class, () -> itemService.create(999, item));
    }

    @Test
    void shouldPatchOnlyProvidedFieldsForOwner() {
        UserDto owner = createUser("owner@example.com");
        ItemDto created = itemService.create(owner.getId(),
                new ItemDto(null, "Дрель", "Старое описание", true));
        ItemDto patch = new ItemDto();

        patch.setDescription("Новое описание");

        ItemDto updated = itemService.update(owner.getId(), created.getId(), patch);

        assertEquals("Дрель", updated.getName());
        assertEquals("Новое описание", updated.getDescription());
        assertTrue(updated.getAvailable());
    }

    @Test
    void shouldRejectUpdateFromNonOwner() {
        UserDto owner = createUser("owner@example.com");
        UserDto anotherUser = createUser("other@example.com");
        ItemDto created = itemService.create(owner.getId(),
                new ItemDto(null, "Дрель", "Ударная дрель", true));

        assertThrows(ForbiddenException.class,
                () -> itemService.update(anotherUser.getId(), created.getId(), new ItemDto()));
    }

    @Test
    void shouldRejectUpdateFromUnknownUser() {
        UserDto owner = createUser("owner@example.com");
        ItemDto created = itemService.create(owner.getId(),
                new ItemDto(null, "Дрель", "Ударная дрель", true));

        assertThrows(NotFoundException.class,
                () -> itemService.update(999, created.getId(), new ItemDto()));
    }

    @Test
    void shouldReturnOnlyOwnersItems() {
        UserDto owner = createUser("owner@example.com");
        UserDto anotherOwner = createUser("other@example.com");
        itemService.create(owner.getId(),
                new ItemDto(null, "Дрель", "Первая", true));
        itemService.create(anotherOwner.getId(),
                new ItemDto(null, "Пила", "Вторая", true));

        List<ItemDto> result = itemService.getByOwner(owner.getId());

        assertEquals(1, result.size());
        assertEquals("Дрель", result.getFirst().getName());
    }

    @Test
    void shouldSearchOnlyAvailableItemsIgnoringCase() {
        UserDto owner = createUser("owner@example.com");
        itemService.create(owner.getId(),
                new ItemDto(null, "Дрель", "Для БЕТОНА", true));
        itemService.create(owner.getId(),
                new ItemDto(null, "Пила", "Для бетона", false));

        List<ItemDto> result = itemService.search("бетона");

        assertEquals(1, result.size());
        assertEquals("Дрель", result.getFirst().getName());
        assertTrue(itemService.search("").isEmpty());
    }

    private UserDto createUser(String email) {
        return userService.create(new UserDto(null, "User", email));
    }
}
