package ru.practicum.shareit.request;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.practicum.shareit.common.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.RequestItemDto;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.request.service.ItemRequestService;
import ru.practicum.shareit.support.AbstractIntegrationTest;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.service.UserService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemRequestServiceTest extends AbstractIntegrationTest {
    @Autowired
    private ItemRequestService service;
    @Autowired
    private ItemRequestRepository requests;
    @Autowired
    private ItemService itemService;
    @Autowired
    private UserService userService;

    @Test
    void createsAndPersistsRequestWithServerTimeAndRequester() {
        User author = user("Автор");
        ItemRequestDto response = service.create(author.getId(), new ItemRequestCreateDto("Нужна дрель"));
        entityManager.flush();
        entityManager.clear();
        ItemRequest stored = requests.findById(response.getId()).orElseThrow();
        assertNotNull(response.getId());
        assertEquals("Нужна дрель", stored.getDescription());
        assertEquals(author.getId(), stored.getRequester().getId());
        assertEquals(NOW, stored.getCreated());
        assertEquals(NOW, response.getCreated());
        assertTrue(response.getItems().isEmpty());
    }

    @Test
    void listsOwnNewestFirstAndUsesIdToBreakTies() {
        User author = user("Автор");
        User other = user("Другой");
        ItemRequest old = requests.saveAndFlush(new ItemRequest(null, "Старый", author, NOW.minusDays(1)));
        ItemRequestDto first = service.create(author.getId(), new ItemRequestCreateDto("Первый"));
        ItemRequestDto second = service.create(author.getId(), new ItemRequestCreateDto("Второй"));
        service.create(other.getId(), new ItemRequestCreateDto("Чужой"));
        assertEquals(List.of(second.getId(), first.getId(), old.getId()),
                service.getOwn(author.getId()).stream().map(ItemRequestDto::getId).toList());
    }

    @Test
    void listsOtherUsersRequestsButNotOwnAndKeepsOrdering() {
        User viewer = user("Читатель");
        User author = user("Автор");
        service.create(viewer.getId(), new ItemRequestCreateDto("Свой"));
        ItemRequest old = requests.saveAndFlush(new ItemRequest(null, "Старый", author, NOW.minusDays(1)));
        ItemRequestDto newer = service.create(author.getId(), new ItemRequestCreateDto("Новый"));
        assertEquals(List.of(newer.getId(), old.getId()),
                service.getOthers(viewer.getId()).stream().map(ItemRequestDto::getId).toList());
    }

    @Test
    void repliesExposeItemIdNameAndOwnerIdWithoutDroppingUnavailableItems() {
        User requester = user("Заказчик");
        User owner = user("Владелец");
        User another = user("Другой владелец");
        ItemRequestDto request = service.create(requester.getId(), new ItemRequestCreateDto("Нужен инструмент"));
        ItemDto first = itemService.create(owner.getId(), new ItemDto(null, "Дрель", "Моя", true, request.getId()));
        ItemDto second = itemService.create(another.getId(), new ItemDto(null, "Пила", "Моя", false, request.getId()));
        itemService.create(owner.getId(), new ItemDto(null, "Без запроса", "Описание", true));
        entityManager.flush();
        entityManager.clear();
        ItemRequestDto result = service.getById(another.getId(), request.getId());
        List<RequestItemDto> replies = result.getItems();
        assertEquals(2, replies.size());
        assertEquals(first.getId(), replies.getFirst().getId());
        assertEquals("Дрель", replies.getFirst().getName());
        assertEquals(owner.getId(), replies.getFirst().getOwnerId());
        assertEquals(second.getId(), replies.getLast().getId());
        assertEquals(another.getId(), replies.getLast().getOwnerId());
        assertEquals(2, service.getOwn(requester.getId()).getFirst().getItems().size());
        assertEquals(2, service.getOthers(owner.getId()).getFirst().getItems().size());
        assertEquals(request.getId(), itemService.getById(owner.getId(), first.getId()).getRequestId());
    }

    @Test
    void creationWithoutRequestIsStillSupported() {
        User owner = user("Владелец");
        ItemDto item = itemService.create(owner.getId(), new ItemDto(null, "Дрель", "Описание", true));
        assertNull(item.getRequestId());
        assertNull(items.findById(item.getId()).orElseThrow().getRequest());
    }

    @Test
    void cannotAttachItemToNonexistentRequest() {
        User owner = user("Владелец");
        assertThrows(NotFoundException.class, () -> itemService.create(owner.getId(),
                new ItemDto(null, "Дрель", "Описание", true, Long.MAX_VALUE)));
        assertTrue(items.findAllByOwnerIdOrderByIdAsc(owner.getId()).isEmpty());
    }

    @Test
    void patchDoesNotChangeOriginalRequestRelationship() {
        User owner = user("Владелец");
        ItemRequestDto original = service.create(owner.getId(), new ItemRequestCreateDto("Оригинал"));
        ItemRequestDto another = service.create(owner.getId(), new ItemRequestCreateDto("Другой"));
        ItemDto item = itemService.create(owner.getId(), new ItemDto(null, "Дрель", "Описание", true, original.getId()));
        ItemDto updated = itemService.update(owner.getId(), item.getId(),
                new ItemDto(null, "Новое имя", null, false, another.getId()));
        assertEquals(original.getId(), updated.getRequestId());
        assertFalse(updated.getAvailable());
        assertTrue(service.getById(owner.getId(), another.getId()).getItems().isEmpty());
    }

    @Test
    void checksMissingUserOnEveryRequestOperation() {
        assertThrows(NotFoundException.class, () -> service.create(Long.MAX_VALUE, new ItemRequestCreateDto("Текст")));
        assertThrows(NotFoundException.class, () -> service.getOwn(Long.MAX_VALUE));
        assertThrows(NotFoundException.class, () -> service.getOthers(Long.MAX_VALUE));
        assertThrows(NotFoundException.class, () -> service.getById(Long.MAX_VALUE, 1L));
    }

    @Test
    void checksMissingRequestAndReturnsEmptyListsForValidUser() {
        User user = user("Пользователь");
        assertThrows(NotFoundException.class, () -> service.getById(user.getId(), Long.MAX_VALUE));
        assertTrue(service.getOwn(user.getId()).isEmpty());
        assertTrue(service.getOthers(user.getId()).isEmpty());
    }

    @Test
    void batchesRepliesWithoutOneQueryPerRequest() {
        User author = user("Автор");
        User owner = user("Владелец");
        for (int i = 0; i < 8; i++) {
            ItemRequestDto request = service.create(author.getId(), new ItemRequestCreateDto("Запрос " + i));
            itemService.create(owner.getId(), new ItemDto(null, "Ответ " + i, "Описание", true, request.getId()));
        }
        entityManager.flush();
        entityManager.clear();
        Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        List<ItemRequestDto> result = service.getOwn(author.getId());
        assertEquals(8, result.size());
        assertTrue(result.stream().allMatch(request -> request.getItems().size() == 1));
        assertTrue(statistics.getPrepareStatementCount() <= 3,
                "Ожидаются проверка пользователя, запросы и одна пакетная выборка ответов");
    }

    @Test
    void deletingRequesterDoesNotDeleteOtherOwnersReply() {
        User author = user("Автор");
        User owner = user("Владелец");
        ItemRequestDto request = service.create(author.getId(), new ItemRequestCreateDto("Запрос"));
        ItemDto reply = itemService.create(owner.getId(), new ItemDto(null, "Ответ", "Описание", true, request.getId()));
        userService.delete(author.getId());
        entityManager.flush();
        entityManager.clear();
        assertFalse(requests.existsById(request.getId()));
        Item stored = items.findById(reply.getId()).orElseThrow();
        assertEquals(owner.getId(), stored.getOwner().getId());
        assertNull(stored.getRequest());
    }
}
