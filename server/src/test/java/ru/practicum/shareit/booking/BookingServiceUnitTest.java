package ru.practicum.shareit.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.booking.service.BookingServiceImpl;
import ru.practicum.shareit.common.exception.BadRequestException;
import ru.practicum.shareit.common.exception.NotFoundException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceUnitTest {
    @Mock
    private BookingRepository bookings;
    @Mock
    private ItemRepository items;
    @Mock
    private UserRepository users;
    private BookingServiceImpl service;
    private final Clock clock = Clock.fixed(Instant.parse("2030-01-15T12:00:00Z"), ZoneOffset.UTC);
    private final User owner = new User(1L, "Владелец", "owner@example.com");
    private final User booker = new User(2L, "Арендатор", "booker@example.com");
    private final Item item = new Item(3L, "Дрель", "Описание", true, owner, null);
    private final LocalDateTime start = LocalDateTime.now(clock).plusDays(1);

    @BeforeEach
    void setUp() {
        service = new BookingServiceImpl(bookings, items, users, clock);
    }

    @Test
    void createsWaitingBookingWithCallerAsBooker() {
        when(users.findById(2L)).thenReturn(Optional.of(booker));
        when(items.findById(3L)).thenReturn(Optional.of(item));
        when(bookings.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            assertEquals(booker, booking.getBooker());
            assertEquals(BookingStatus.WAITING, booking.getStatus());
            booking.setId(4L);
            return booking;
        });
        assertEquals(4L, service.create(2L, new BookingRequestDto(3L, start, start.plusDays(1))).getId());
    }

    @Test
    void readsBookingOnlyAfterAcquiringItemLock() {
        Booking booking = new Booking(4L, start, start.plusDays(1), item, booker, BookingStatus.WAITING);
        when(bookings.findItemId(4L)).thenReturn(Optional.of(3L));
        when(items.findLockedById(3L)).thenReturn(Optional.of(item));
        when(bookings.findById(4L)).thenReturn(Optional.of(booking));
        assertEquals(BookingStatus.APPROVED, service.approve(1L, 4L, true).getStatus());
        InOrder order = inOrder(bookings, items);
        order.verify(bookings).findItemId(4L);
        order.verify(items).findLockedById(3L);
        order.verify(bookings).findById(4L);
        order.verify(bookings).existsOverlapping(3L, 4L, BookingStatus.APPROVED, start, start.plusDays(1));
        order.verify(bookings).flush();
    }

    @Test
    void rejectsMissingItemBetweenBookingLookupAndLock() {
        when(bookings.findItemId(4L)).thenReturn(Optional.of(3L));
        assertThrows(NotFoundException.class, () -> service.approve(1L, 4L, true));
        verify(bookings, never()).findById(4L);
    }

    @Test
    void overlappingApprovalDoesNotMutateWaitingStatus() {
        Booking booking = new Booking(4L, start, start.plusDays(1), item, booker, BookingStatus.WAITING);
        when(bookings.findItemId(4L)).thenReturn(Optional.of(3L));
        when(items.findLockedById(3L)).thenReturn(Optional.of(item));
        when(bookings.findById(4L)).thenReturn(Optional.of(booking));
        when(bookings.existsOverlapping(3L, 4L, BookingStatus.APPROVED, start, start.plusDays(1))).thenReturn(true);
        assertThrows(BadRequestException.class, () -> service.approve(1L, 4L, true));
        assertEquals(BookingStatus.WAITING, booking.getStatus());
        verify(bookings, never()).flush();
    }
}
