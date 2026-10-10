package ru.practicum.shareit.item;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.practicum.shareit.common.exception.ForbiddenException;
import ru.practicum.shareit.common.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemRequestDto;
import ru.practicum.shareit.item.dto.ItemResponseDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.support.AbstractIntegrationTest;
import ru.practicum.shareit.user.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemServiceTest extends AbstractIntegrationTest {
    @Autowired
    private ItemService service;

    @Test
    void shouldCreateItemWithOwnerAndGeneratedId() {
        User owner = user("Владелец");
        ItemDto result = service.create(owner.getId(), new ItemRequestDto("Дрель", "Для бетона", true));
        assertNotEquals(Long.MAX_VALUE, result.getId().longValue());
        entityManager.clear();
        Item stored = items.findById(result.getId()).orElseThrow();
        assertEquals(owner.getId(), stored.getOwner().getId());
        assertEquals("Дрель", service.getById(owner.getId(), result.getId()).getName());
    }

    @Test
    void shouldRejectUnknownOwnerOnCreate() {
        assertThrows(NotFoundException.class,
                () -> service.create(Long.MAX_VALUE, new ItemRequestDto("Дрель", "Для бетона", true)));
    }

    @Test
    void shouldPatchNonNullFieldsAndPreserveOthers() {
        User owner = user("Владелец");
        Item stored = item(owner, "Дрель", true);
        ItemDto updated = service.update(owner.getId(), stored.getId(), new ItemRequestDto("Пила", null, false));
        assertEquals("Пила", updated.getName());
        assertEquals(stored.getDescription(), updated.getDescription());
        assertFalse(updated.getAvailable());
        ItemDto next = service.update(owner.getId(), stored.getId(), new ItemRequestDto(null, "Новое", null));
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
                () -> service.update(other.getId(), stored.getId(), new ItemRequestDto("Пила", null, null)));
    }

    @Test
    void shouldRejectUnknownUserOnUpdate() {
        Item stored = item(user("Владелец"), "Дрель", true);
        assertThrows(NotFoundException.class,
                () -> service.update(Long.MAX_VALUE, stored.getId(), new ItemRequestDto("Пила", null, null)));
    }

    @Test
    void shouldRejectMissingItemOnUpdate() {
        User owner = user("Владелец");
        assertThrows(NotFoundException.class,
                () -> service.update(owner.getId(), Long.MAX_VALUE, new ItemRequestDto("Пила", null, null)));
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
        service.create(owner.getId(), new ItemRequestDto("Дрель", "Для БЕТОНА", true));
        service.create(owner.getId(), new ItemRequestDto("Пила", "Для бетона", false));
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

    @Test
    void creationWithoutRequestIsStillSupported() {
        User owner = user("Владелец");
        ItemDto result = service.create(owner.getId(), new ItemRequestDto("Дрель", "Описание", true));
        entityManager.flush();
        entityManager.clear();
        assertNull(result.getRequestId());
        assertNull(entityManager.find(Item.class, result.getId()).getRequest());
    }

    @Test
    void cannotAttachItemToNonexistentRequest() {
        User owner = user("Владелец");
        assertThrows(NotFoundException.class, () -> service.create(owner.getId(),
                new ItemRequestDto("Дрель", "Описание", true, Long.MAX_VALUE)));
        assertTrue(items.findAllByOwnerIdOrderByIdAsc(owner.getId()).isEmpty());
    }

    @Test
    void createsItemInResponseToRequest() {
        User requester = user("Заказчик");
        User owner = user("Владелец");
        ItemRequest request = new ItemRequest(null, "Нужна дрель", requester, NOW);
        entityManager.persist(request);
        ItemDto response = service.create(owner.getId(),
                new ItemRequestDto("Дрель", "Описание", true, request.getId()));
        entityManager.flush();
        entityManager.clear();
        Item stored = entityManager.find(Item.class, response.getId());
        assertEquals(request.getId(), stored.getRequest().getId());
        assertEquals(owner.getId(), stored.getOwner().getId());
        assertEquals(request.getId(), response.getRequestId());
        assertEquals(request.getId(), service.getById(owner.getId(), response.getId()).getRequestId());
    }

    @Test
    void patchDoesNotChangeOriginalRequestRelationship() {
        User owner = user("Владелец");
        ItemRequest original = new ItemRequest(null, "Оригинал", owner, NOW);
        ItemRequest another = new ItemRequest(null, "Другой", owner, NOW);
        entityManager.persist(original);
        entityManager.persist(another);
        Item existing = new Item(null, "Дрель", "Описание", true, owner, original);
        entityManager.persist(existing);
        ItemDto updated = service.update(owner.getId(), existing.getId(),
                new ItemRequestDto("Новое имя", null, false, another.getId()));
        entityManager.flush();
        entityManager.clear();
        assertEquals(original.getId(), updated.getRequestId());
        assertFalse(updated.getAvailable());
        assertEquals(original.getId(), entityManager.find(Item.class, existing.getId()).getRequest().getId());
        Long otherReplies = entityManager.createQuery(
                "select count(i) from Item i where i.request.id = :requestId", Long.class)
                .setParameter("requestId", another.getId()).getSingleResult();
        assertEquals(0L, otherReplies.longValue());
    }
}
