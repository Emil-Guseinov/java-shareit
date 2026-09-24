package ru.practicum.shareit.booking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.common.exception.BadRequestException;
import ru.practicum.shareit.common.exception.NotFoundException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.support.AbstractIntegrationTest;
import ru.practicum.shareit.user.User;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingQueryTest extends AbstractIntegrationTest {
    @Autowired
    private BookingService service;

    @ParameterizedTest
    @EnumSource(BookingState.class)
    void shouldFilterBothViewsAndSortByStartDescending(BookingState state) {
        User owner = user("Владелец");
        User booker = user("Арендатор");
        Item first = item(owner, "Дрель", true);
        Item second = item(owner, "Пила", true);
        List<Booking> ownBookings = List.of(
                booking(first, booker, NOW.minusDays(3), NOW.minusDays(2), BookingStatus.APPROVED),
                booking(first, booker, NOW.minusHours(1), NOW.plusHours(1), BookingStatus.APPROVED),
                booking(second, booker, NOW.plusDays(1), NOW.plusDays(2), BookingStatus.APPROVED),
                booking(first, booker, NOW.plusDays(3), NOW.plusDays(4), BookingStatus.WAITING),
                booking(second, booker, NOW.plusDays(3), NOW.plusDays(4), BookingStatus.REJECTED)
        );
        booking(item(user("Чужой владелец"), "Чужая", true), user("Чужой арендатор"),
                NOW.plusDays(10), NOW.plusDays(11), BookingStatus.WAITING);
        List<Long> expected = ownBookings.stream().filter(b -> matches(b, state))
                .sorted(Comparator.comparing(Booking::getStart).thenComparing(Booking::getId).reversed())
                .map(Booking::getId).toList();
        entityManager.clear();
        assertEquals(expected, service.getByBooker(booker.getId(), state).stream().map(BookingDto::getId).toList());
        assertEquals(expected, service.getByOwner(owner.getId(), state).stream().map(BookingDto::getId).toList());
    }

    @Test
    void shouldHandleExactTemporalBoundariesWithoutDoubleCounting() {
        User booker = user("Арендатор");
        Item item = item(user("Владелец"), "Дрель", true);
        Booking ended = booking(item, booker, NOW.minusHours(1), NOW, BookingStatus.APPROVED);
        Booking started = booking(item, booker, NOW, NOW.plusHours(1), BookingStatus.APPROVED);
        assertEquals(List.of(ended.getId()), service.getByBooker(booker.getId(), BookingState.PAST)
                .stream().map(BookingDto::getId).toList());
        assertEquals(List.of(started.getId()), service.getByBooker(booker.getId(), BookingState.CURRENT)
                .stream().map(BookingDto::getId).toList());
        assertTrue(service.getByBooker(booker.getId(), BookingState.FUTURE).isEmpty());
    }

    @Test
    void shouldReturnEmptyListForExistingUserWithoutBookings() {
        User existing = user("Без бронирований");
        assertTrue(service.getByBooker(existing.getId(), BookingState.ALL).isEmpty());
        assertTrue(service.getByOwner(existing.getId(), BookingState.ALL).isEmpty());
    }

    @Test
    void shouldRejectUnknownBooker() {
        assertThrows(NotFoundException.class, () -> service.getByBooker(Long.MAX_VALUE, BookingState.ALL));
    }

    @Test
    void shouldRejectUnknownOwner() {
        assertThrows(NotFoundException.class, () -> service.getByOwner(Long.MAX_VALUE, BookingState.ALL));
    }

    @ParameterizedTest
    @EnumSource(BookingState.class)
    void shouldParseEveryState(BookingState state) {
        assertEquals(state, BookingState.from(state.name()));
    }

    @Test
    void shouldRejectNullAndUnknownState() {
        assertThrows(BadRequestException.class, () -> BookingState.from(null));
        assertThrows(BadRequestException.class, () -> BookingState.from("UNKNOWN"));
    }

    private boolean matches(Booking booking, BookingState state) {
        return switch (state) {
            case ALL -> true;
            case CURRENT -> !booking.getStart().isAfter(NOW) && booking.getEnd().isAfter(NOW);
            case PAST -> !booking.getEnd().isAfter(NOW);
            case FUTURE -> booking.getStart().isAfter(NOW);
            case WAITING -> booking.getStatus() == BookingStatus.WAITING;
            case REJECTED -> booking.getStatus() == BookingStatus.REJECTED;
        };
    }
}
