package ru.practicum.shareit.item;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.item.dto.ItemResponseDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.support.AbstractIntegrationTest;
import ru.practicum.shareit.user.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryCountTest extends AbstractIntegrationTest {
    @Autowired
    private ItemService service;

    @Test
    void shouldUseBoundedQueriesForOwnerListWithBookingsAndComments() {
        User owner = user("Владелец");
        for (int index = 0; index < 8; index++) {
            User author = user("Арендатор " + index);
            Item item = item(owner, "Вещь " + index, true);
            booking(item, author, NOW.minusDays(2), NOW.minusDays(1), BookingStatus.APPROVED);
            booking(item, author, NOW.plusDays(1), NOW.plusDays(2), BookingStatus.APPROVED);
            comments.save(new Comment(null, "Отзыв " + index, item, author, NOW.minusHours(1)));
        }
        entityManager.flush();
        entityManager.clear();
        Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        List<ItemResponseDto> result = service.getByOwner(owner.getId());
        assertEquals(8, result.size());
        result.forEach(dto -> {
            assertEquals(1, dto.getComments().size());
            assertTrue(dto.getLastBooking().getBookerId() > 0);
            assertTrue(dto.getNextBooking().getBookerId() > 0);
        });
        long count = statistics.getPrepareStatementCount();
        assertTrue(count <= 5, "Ожидается не более 5 SQL-запросов, выполнено: " + count);
    }
}
