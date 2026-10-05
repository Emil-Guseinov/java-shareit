package ru.practicum.shareit.booking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.common.exception.BadRequestException;
import ru.practicum.shareit.common.exception.ForbiddenException;
import ru.practicum.shareit.common.exception.NotFoundException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.support.AbstractIntegrationTest;
import ru.practicum.shareit.user.User;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookingServiceTest extends AbstractIntegrationTest {
    @Autowired
    private BookingService service;

    @Test
    void shouldCreateWaitingBooking() {
        User booker = user("Арендатор");
        Item item = item(user("Владелец"), "Дрель", true);
        BookingDto result = service.create(booker.getId(), request(item));
        assertEquals(BookingStatus.WAITING, result.getStatus());
        assertEquals(booker.getId(), result.getBooker().getId());
        assertEquals(item.getId(), result.getItem().getId());
        assertEquals("Дрель", result.getItem().getName());
    }

    @Test
    void shouldRejectUnavailableItem() {
        User booker = user("Арендатор");
        Item item = item(user("Владелец"), "Дрель", false);
        assertThrows(BadRequestException.class, () -> service.create(booker.getId(), request(item)));
    }

    @Test
    void shouldRejectOwnItem() {
        User owner = user("Владелец");
        Item item = item(owner, "Дрель", true);
        assertThrows(BadRequestException.class, () -> service.create(owner.getId(), request(item)));
    }

    @Test
    void shouldRejectUnknownBooker() {
        Item item = item(user("Владелец"), "Дрель", true);
        assertThrows(NotFoundException.class, () -> service.create(Long.MAX_VALUE, request(item)));
    }

    @Test
    void shouldRejectUnknownItem() {
        User booker = user("Арендатор");
        BookingRequestDto dto = new BookingRequestDto(Long.MAX_VALUE, NOW.plusDays(1), NOW.plusDays(2));
        assertThrows(NotFoundException.class, () -> service.create(booker.getId(), dto));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void shouldRejectNonFutureStart(int days) {
        BookingRequestDto dto = new BookingRequestDto(1L, NOW.plusDays(days), NOW.plusDays(2));
        assertThrows(BadRequestException.class, () -> service.create(1L, dto));
    }

    @Test
    void shouldRejectMissingDates() {
        assertThrows(BadRequestException.class,
                () -> service.create(1L, new BookingRequestDto(1L, null, NOW.plusDays(2))));
    }

    @Test
    void shouldRejectMissingEnd() {
        assertThrows(BadRequestException.class,
                () -> service.create(1L, new BookingRequestDto(1L, NOW.plusDays(1), null)));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void shouldRejectEndNotAfterStart(int days) {
        LocalDateTime start = NOW.plusDays(2);
        assertThrows(BadRequestException.class,
                () -> service.create(1L, new BookingRequestDto(1L, start, start.plusDays(days))));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldAllowOwnerToDecide(boolean approved) {
        User owner = user("Владелец");
        Item item = item(owner, "Дрель", true);
        Booking booking = booking(item, user("Арендатор"), NOW.plusDays(1), NOW.plusDays(2), BookingStatus.WAITING);
        BookingDto result = service.approve(owner.getId(), booking.getId(), approved);
        assertEquals(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED, result.getStatus());
        entityManager.clear();
        assertEquals(result.getStatus(), bookings.findById(booking.getId()).orElseThrow().getStatus());
    }

    @Test
    void shouldRejectRepeatedDecision() {
        User owner = user("Владелец");
        Item item = item(owner, "Дрель", true);
        Booking booking = booking(item, user("Арендатор"), NOW.plusDays(1), NOW.plusDays(2), BookingStatus.APPROVED);
        assertThrows(BadRequestException.class, () -> service.approve(owner.getId(), booking.getId(), true));
    }

    @Test
    void shouldRejectDecisionByNonOwnerEvenWhenUserDoesNotExist() {
        Item item = item(user("Владелец"), "Дрель", true);
        Booking booking = booking(item, user("Арендатор"), NOW.plusDays(1), NOW.plusDays(2), BookingStatus.WAITING);
        assertThrows(ForbiddenException.class, () -> service.approve(Long.MAX_VALUE, booking.getId(), true));
    }

    @Test
    void shouldRejectApprovalAfterItemBecameUnavailable() {
        User owner = user("Владелец");
        Item item = item(owner, "Дрель", false);
        Booking booking = booking(item, user("Арендатор"), NOW.plusDays(1), NOW.plusDays(2), BookingStatus.WAITING);
        assertThrows(BadRequestException.class, () -> service.approve(owner.getId(), booking.getId(), true));
    }

    @Test
    void shouldAllowRejectingUnavailableItemRequest() {
        User owner = user("Владелец");
        Item item = item(owner, "Дрель", false);
        Booking booking = booking(item, user("Арендатор"), NOW.plusDays(1), NOW.plusDays(2), BookingStatus.WAITING);
        assertEquals(BookingStatus.REJECTED, service.approve(owner.getId(), booking.getId(), false).getStatus());
    }

    @Test
    void shouldRejectUnknownBookingOnDecision() {
        assertThrows(NotFoundException.class, () -> service.approve(1L, Long.MAX_VALUE, true));
    }

    @Test
    void shouldRejectOverlappingApprovedBooking() {
        User owner = user("Владелец");
        User booker = user("Арендатор");
        Item item = item(owner, "Дрель", true);
        booking(item, booker, NOW.plusDays(1), NOW.plusDays(3), BookingStatus.APPROVED);
        Booking overlap = booking(item, booker, NOW.plusDays(2), NOW.plusDays(4), BookingStatus.WAITING);
        assertThrows(BadRequestException.class, () -> service.approve(owner.getId(), overlap.getId(), true));
    }

    @Test
    void shouldAllowAdjacentBookingsAndIgnoreRejectedOverlap() {
        User owner = user("Владелец");
        User booker = user("Арендатор");
        Item item = item(owner, "Дрель", true);
        booking(item, booker, NOW.plusDays(1), NOW.plusDays(2), BookingStatus.APPROVED);
        booking(item, booker, NOW.plusDays(2), NOW.plusDays(4), BookingStatus.REJECTED);
        Booking adjacent = booking(item, booker, NOW.plusDays(2), NOW.plusDays(3), BookingStatus.WAITING);
        assertEquals(BookingStatus.APPROVED, service.approve(owner.getId(), adjacent.getId(), true).getStatus());
    }

    @Test
    void shouldAllowOnlyOwnerAndBookerToReadBooking() {
        User owner = user("Владелец");
        User booker = user("Арендатор");
        Item item = item(owner, "Дрель", true);
        Booking booking = booking(item, booker, NOW.plusDays(1), NOW.plusDays(2), BookingStatus.WAITING);
        assertEquals(booking.getId(), service.getById(owner.getId(), booking.getId()).getId());
        assertEquals(booking.getId(), service.getById(booker.getId(), booking.getId()).getId());
        assertThrows(ForbiddenException.class, () -> service.getById(Long.MAX_VALUE, booking.getId()));
    }

    @Test
    void shouldRejectUnknownBookingOnRead() {
        assertThrows(NotFoundException.class, () -> service.getById(1L, Long.MAX_VALUE));
    }

    private BookingRequestDto request(Item item) {
        return new BookingRequestDto(item.getId(), NOW.plusDays(1), NOW.plusDays(2));
    }
}
