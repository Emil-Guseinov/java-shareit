package ru.practicum.shareit.item;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.practicum.shareit.common.exception.ForbiddenException;
import ru.practicum.shareit.common.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemResponseDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.support.AbstractIntegrationTest;
import ru.practicum.shareit.user.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemServiceTest extends AbstractIntegrationTest {
    @Autowired
    private ItemService service;

    @Test
    void shouldCreateItemWithOwnerAndIgnoreClientId() {
        User owner = user("Владелец");
        ItemDto result = service.create(owner.getId(), new ItemDto(Long.MAX_VALUE, "Дрель", "Для бетона", true));
        assertNotEquals(Long.MAX_VALUE, result.getId().longValue());
        entityManager.clear();
        Item stored = items.findById(result.getId()).orElseThrow();
        assertEquals(owner.getId(), stored.getOwner().getId());
        assertEquals("Дрель", service.getById(owner.getId(), result.getId()).getName());
    }

    @Test
    void shouldRejectUnknownOwnerOnCreate() {
        assertThrows(NotFoundException.class,
                () -> service.create(Long.MAX_VALUE, new ItemDto(null, "Дрель", "Для бетона", true)));
    }

    @Test
    void shouldPatchNonNullFieldsAndPreserveOthers() {
        User owner = user("Владелец");
        Item stored = item(owner, "Дрель", true);
        ItemDto updated = service.update(owner.getId(), stored.getId(), new ItemDto(null, "Пила", null, false));
        assertEquals("Пила", updated.getName());
        assertEquals(stored.getDescription(), updated.getDescription());
        assertFalse(updated.getAvailable());
        ItemDto next = service.update(owner.getId(), stored.getId(), new ItemDto(null, null, "Новое", null));
        assertEquals("Пила", next.getName());
        assertEquals("Новое", next.getDescription());
        assertFalse(next.getAvailable());
    }

    @Test
    void shouldRejectAnotherUserOnUpdate() {
        User owner = user("Владелец");
        User other = user("Другой");
        Item stored = item(owner, "Дрель", true);
        assertThrows(ForbiddenException.class,
                () -> service.update(other.getId(), stored.getId(), new ItemDto(null, "Пила", null, null)));
    }

    @Test
    void shouldRejectUnknownUserOnUpdate() {
        Item stored = item(user("Владелец"), "Дрель", true);
        assertThrows(NotFoundException.class,
                () -> service.update(Long.MAX_VALUE, stored.getId(), new ItemDto(null, "Пила", null, null)));
    }

    @Test
    void shouldRejectMissingItemOnUpdate() {
        User owner = user("Владелец");
        assertThrows(NotFoundException.class,
                () -> service.update(owner.getId(), Long.MAX_VALUE, new ItemDto(null, "Пила", null, null)));
    }

    @Test
    void shouldRejectMissingItemOnRead() {
        assertThrows(NotFoundException.class, () -> service.getById(1L, Long.MAX_VALUE));
    }

    @Test
    void shouldReturnOnlyOwnersItemsInIdOrder() {
        User owner = user("Владелец");
        Item first = item(owner, "Первая", true);
        Item second = item(owner, "Вторая", false);
        item(user("Другой"), "Чужая", true);
        List<Long> result = service.getByOwner(owner.getId()).stream().map(ItemResponseDto::getId).toList();
        assertEquals(List.of(first.getId(), second.getId()), result);
    }

    @Test
    void shouldReturnEmptyListForOwnerWithoutItems() {
        assertTrue(service.getByOwner(user("Владелец").getId()).isEmpty());
    }

    @Test
    void shouldRejectMissingUserOnOwnerList() {
        assertThrows(NotFoundException.class, () -> service.getByOwner(Long.MAX_VALUE));
    }

    @Test
    void shouldSearchOnlyAvailableItemsIgnoringCase() {
        User owner = user("Владелец");
        service.create(owner.getId(), new ItemDto(null, "Дрель", "Для БЕТОНА", true));
        service.create(owner.getId(), new ItemDto(null, "Пила", "Для бетона", false));
        List<ItemDto> result = service.search("бетона");
        assertEquals(1, result.size());
        assertEquals("Дрель", result.getFirst().getName());
        assertEquals(1, service.search("ДРЕЛЬ").size());
        assertTrue(service.search("Не существующее описание").isEmpty());
    }

    @Test
    void shouldSearchPercentAndUnderscoreAsLiteralCharacters() {
        User owner = user("Владелец");
        item(owner, "Точность 100%", true);
        item(owner, "Модель_1", true);
        item(owner, "Обычная вещь", true);
        assertEquals(1, service.search("%").size());
        assertEquals(1, service.search("_").size());
    }
}
