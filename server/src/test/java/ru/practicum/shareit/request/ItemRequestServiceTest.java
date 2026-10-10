package ru.practicum.shareit.request;

import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.common.exception.NotFoundException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.RequestItemDto;
import ru.practicum.shareit.request.service.ItemRequestService;
import ru.practicum.shareit.user.User;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ItemRequestServiceTest {
    private static final LocalDateTime DATE = LocalDateTime.of(2030, 1, 15, 12, 0);

    @Autowired
    private ItemRequestService service;
    @Autowired
    private EntityManager entityManager;

    @Test
    void createsAndPersistsRequestWithServerTimeAndRequester() {
        User author = user("Автор");
        LocalDateTime before = LocalDateTime.now();
        ItemRequestDto response = service.create(author.getId(), new ItemRequestCreateDto("Нужна дрель"));
        LocalDateTime after = LocalDateTime.now();
        assertNotNull(response.getId());
        assertNotNull(response.getCreated());
        assertFalse(response.getCreated().isBefore(before));
        assertFalse(response.getCreated().isAfter(after));
        assertTrue(response.getItems().isEmpty());
        entityManager.flush();
        entityManager.clear();
        ItemRequest stored = entityManager.find(ItemRequest.class, response.getId());
        assertNotNull(stored);
        assertEquals("Нужна дрель", stored.getDescription());
        assertEquals(author.getId(), stored.getRequester().getId());
        // TIMESTAMP(6) может округлить наносекунды до микросекунд.
        assertTrue(Duration.between(response.getCreated(), stored.getCreated()).abs().toNanos() <= 1_000);
    }

    @Test
    void listsOwnNewestFirstAndUsesIdToBreakTies() {
        User author = user("Автор");
        User other = user("Другой");
        ItemRequest old = request(author, "Старый", DATE.minusDays(1));
        ItemRequest first = request(author, "Первый", DATE);
        ItemRequest second = request(author, "Второй", DATE);
        request(other, "Чужой", DATE.plusDays(1));
        flushAndClear();
        assertEquals(List.of(second.getId(), first.getId(), old.getId()),
                service.getOwn(author.getId()).stream().map(ItemRequestDto::getId).toList());
    }

    @Test
    void listsOtherUsersRequestsButNotOwnAndKeepsOrdering() {
        User viewer = user("Читатель");
        User author = user("Автор");
        request(viewer, "Свой", DATE);
        ItemRequest old = request(author, "Старый", DATE.minusDays(1));
        ItemRequest newer = request(author, "Новый", DATE);
        flushAndClear();
        assertEquals(List.of(newer.getId(), old.getId()),
                service.getOthers(viewer.getId()).stream().map(ItemRequestDto::getId).toList());
    }

    @Test
    void returnsReplyIdsNamesAndOwnersForAnyExistingUser() {
        User requester = user("Заказчик");
        User owner = user("Владелец");
        User another = user("Другой владелец");
        ItemRequest request = request(requester, "Нужен инструмент", DATE);
        Item first = reply(owner, "Дрель", true, request);
        Item second = reply(another, "Пила", false, request);
        reply(owner, "Без запроса", true, null);
        flushAndClear();
        ItemRequestDto result = service.getById(another.getId(), request.getId());
        List<RequestItemDto> replies = result.getItems();
        assertEquals(request.getId(), result.getId());
        assertEquals("Нужен инструмент", result.getDescription());
        assertEquals(DATE, result.getCreated());
        assertEquals(2, replies.size());
        assertEquals(first.getId(), replies.getFirst().getId());
        assertEquals("Дрель", replies.getFirst().getName());
        assertEquals(owner.getId(), replies.getFirst().getOwnerId());
        assertEquals(second.getId(), replies.getLast().getId());
        assertEquals("Пила", replies.getLast().getName());
        assertEquals(another.getId(), replies.getLast().getOwnerId());
    }

    @Test
    void ownRequestsIncludeReplies() {
        User requester = user("Заказчик");
        User owner = user("Владелец");
        ItemRequest request = request(requester, "Нужна дрель", DATE);
        Item item = reply(owner, "Дрель", true, request);
        flushAndClear();
        ItemRequestDto result = service.getOwn(requester.getId()).getFirst();
        assertEquals(request.getId(), result.getId());
        assertEquals(item.getId(), result.getItems().getFirst().getId());
        assertEquals(owner.getId(), result.getItems().getFirst().getOwnerId());
    }

    @Test
    void otherRequestsIncludeReplies() {
        User requester = user("Заказчик");
        User owner = user("Владелец");
        ItemRequest request = request(requester, "Нужна дрель", DATE);
        Item item = reply(owner, "Дрель", true, request);
        flushAndClear();
        ItemRequestDto result = service.getOthers(owner.getId()).getFirst();
        assertEquals(request.getId(), result.getId());
        assertEquals(item.getId(), result.getItems().getFirst().getId());
        assertEquals(owner.getId(), result.getItems().getFirst().getOwnerId());
    }

    @Test
    void createRejectsMissingUser() {
        assertThrows(NotFoundException.class, () -> service.create(Long.MAX_VALUE, new ItemRequestCreateDto("Текст")));
    }

    @Test
    void getOwnRejectsMissingUser() {
        assertThrows(NotFoundException.class, () -> service.getOwn(Long.MAX_VALUE));
    }

    @Test
    void getOthersRejectsMissingUser() {
        assertThrows(NotFoundException.class, () -> service.getOthers(Long.MAX_VALUE));
    }

    @Test
    void getByIdRejectsMissingUser() {
        assertThrows(NotFoundException.class, () -> service.getById(Long.MAX_VALUE, 1L));
    }

    @Test
    void getByIdRejectsMissingRequest() {
        User viewer = user("Читатель");
        assertThrows(NotFoundException.class, () -> service.getById(viewer.getId(), Long.MAX_VALUE));
    }

    @Test
    void getOwnReturnsEmptyList() {
        assertTrue(service.getOwn(user("Пользователь").getId()).isEmpty());
    }

    @Test
    void getOthersReturnsEmptyList() {
        assertTrue(service.getOthers(user("Пользователь").getId()).isEmpty());
    }

    @Test
    void getByIdReturnsEmptyAnswers() {
        User viewer = user("Пользователь");
        ItemRequest request = request(viewer, "Запрос", DATE);
        flushAndClear();
        assertTrue(service.getById(viewer.getId(), request.getId()).getItems().isEmpty());
    }

    @Test
    void batchesRepliesWithoutOneQueryPerRequest() {
        User author = user("Автор");
        User owner = user("Владелец");
        for (int i = 0; i < 8; i++) {
            ItemRequest request = request(author, "Запрос " + i, DATE.plusSeconds(i));
            reply(owner, "Ответ " + i, true, request);
        }
        flushAndClear();
        Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        List<ItemRequestDto> result = service.getOwn(author.getId());
        assertEquals(8, result.size());
        assertTrue(result.stream().allMatch(request -> request.getItems().size() == 1));
        assertTrue(statistics.getPrepareStatementCount() <= 3,
                "Ожидаются проверка пользователя, запросы и одна пакетная выборка ответов");
    }

    private User user(String name) {
        User user = new User(null, name, UUID.randomUUID() + "@example.com");
        entityManager.persist(user);
        return user;
    }

    private ItemRequest request(User requester, String description, LocalDateTime created) {
        ItemRequest request = new ItemRequest(null, description, requester, created);
        entityManager.persist(request);
        return request;
    }

    private Item reply(User owner, String name, boolean available, ItemRequest request) {
        Item item = new Item(null, name, "Описание", available, owner, request);
        entityManager.persist(item);
        return item;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
