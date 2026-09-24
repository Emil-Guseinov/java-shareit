package ru.practicum.shareit.item;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.item.dto.ItemResponseDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.support.AbstractIntegrationTest;
import ru.practicum.shareit.user.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemBookingViewTest extends AbstractIntegrationTest {
    @Autowired
    private ItemService service;

    @Test
    void shouldChooseNearestApprovedBookingsOnly() {
        User owner = user("Владелец");
        User booker = user("Арендатор");
        Item item = item(owner, "Дрель", true);
        booking(item, booker, NOW.minusDays(10), NOW.minusDays(9), BookingStatus.APPROVED);
        Booking last = booking(item, booker, NOW.minusDays(3), NOW.minusDays(2), BookingStatus.APPROVED);
        booking(item, booker, NOW.minusDays(1), NOW.minusHours(2), BookingStatus.REJECTED);
        booking(item, booker, NOW.plusHours(1), NOW.plusHours(2), BookingStatus.WAITING);
        Booking next = booking(item, booker, NOW.plusDays(2), NOW.plusDays(3), BookingStatus.APPROVED);
        booking(item, booker, NOW.plusDays(5), NOW.plusDays(6), BookingStatus.APPROVED);
        entityManager.clear();
        ItemResponseDto result = service.getById(owner.getId(), item.getId());
        assertEquals(last.getId(), result.getLastBooking().getId());
        assertEquals(next.getId(), result.getNextBooking().getId());
        assertEquals(booker.getId(), result.getNextBooking().getBookerId());
        assertEquals(NOW.plusDays(2), result.getNextBooking().getStart());
        assertEquals(NOW.plusDays(3), result.getNextBooking().getEnd());
    }

    @Test
    void shouldTreatCurrentBookingAsLastAndHideBookingsFromOtherReaders() {
        User owner = user("Владелец");
        User booker = user("Арендатор");
        Item item = item(owner, "Дрель", true);
        Booking current = booking(item, booker, NOW, NOW.plusDays(1), BookingStatus.APPROVED);
        assertEquals(current.getId(), service.getById(owner.getId(), item.getId()).getLastBooking().getId());
        ItemResponseDto otherView = service.getById(booker.getId(), item.getId());
        assertNull(otherView.getLastBooking());
        assertNull(otherView.getNextBooking());
    }

    @Test
    void shouldKeepItemsWithoutBookingsAndReturnStableTieBreaks() {
        User owner = user("Владелец");
        User booker = user("Арендатор");
        Item first = item(owner, "Первая", true);
        Item empty = item(owner, "Вторая", true);
        booking(first, booker, NOW.minusDays(2), NOW.minusDays(1), BookingStatus.APPROVED);
        Booking last = booking(first, booker, NOW.minusDays(2), NOW.minusDays(1), BookingStatus.APPROVED);
        Booking next = booking(first, booker, NOW.plusDays(2), NOW.plusDays(3), BookingStatus.APPROVED);
        booking(first, booker, NOW.plusDays(2), NOW.plusDays(3), BookingStatus.APPROVED);
        List<ItemResponseDto> result = service.getByOwner(owner.getId());
        assertEquals(2, result.size());
        assertEquals(last.getId(), result.getFirst().getLastBooking().getId());
        assertEquals(next.getId(), result.getFirst().getNextBooking().getId());
        assertEquals(empty.getId(), result.get(1).getId());
        assertNull(result.get(1).getLastBooking());
        assertNull(result.get(1).getNextBooking());
        assertTrue(result.get(1).getComments().isEmpty());
    }
}
