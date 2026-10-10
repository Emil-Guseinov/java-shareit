package ru.practicum.shareit.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.common.exception.BadRequestException;
import ru.practicum.shareit.common.exception.NotFoundException;
import ru.practicum.shareit.item.dto.CommentRequestDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemRequestDto;
import ru.practicum.shareit.item.dto.ItemResponseDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.item.service.ItemServiceImpl;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceUnitTest {
    @Mock
    private ItemRepository items;
    @Mock
    private UserRepository users;
    @Mock
    private BookingRepository bookings;
    @Mock
    private CommentRepository comments;
    @Mock
    private ItemRequestRepository requests;
    private ItemServiceImpl service;
    private final Clock clock = Clock.fixed(Instant.parse("2030-01-15T12:00:00Z"), ZoneOffset.UTC);
    private final User owner = new User(1L, "Владелец", "owner@example.com");

    @BeforeEach
    void setUp() {
        service = new ItemServiceImpl(items, users, bookings, comments, clock, requests);
    }

    @Test
    void linksReplyToExistingRequestAndGeneratesId() {
        ItemRequest request = new ItemRequest(3L, "Запрос", owner, LocalDateTime.now(clock));
        when(users.findById(1L)).thenReturn(Optional.of(owner));
        when(requests.findById(3L)).thenReturn(Optional.of(request));
        when(items.save(any(Item.class))).thenAnswer(invocation -> {
            Item item = invocation.getArgument(0);
            assertNull(item.getId());
            assertEquals(request, item.getRequest());
            assertEquals(owner, item.getOwner());
            item.setId(2L);
            return item;
        });
        assertEquals(3L, service.create(1L, new ItemRequestDto("Дрель", "Описание", true, 3L)).getRequestId());
    }

    @Test
    void missingRequestPreventsItemSave() {
        when(users.findById(1L)).thenReturn(Optional.of(owner));
        assertThrows(NotFoundException.class, () -> service.create(1L, new ItemRequestDto("Дрель", "Описание", true, 3L)));
        verifyNoInteractions(items);
    }

    @Test
    void patchChangesFalseButNotOmittedFields() {
        Item item = new Item(2L, "Дрель", "Описание", true, owner, null);
        when(users.existsById(1L)).thenReturn(true);
        when(items.findLockedById(2L)).thenReturn(Optional.of(item));
        ItemDto result = service.update(1L, 2L, new ItemRequestDto(null, null, false));
        assertFalse(result.getAvailable());
        assertEquals("Дрель", result.getName());
        assertEquals("Описание", result.getDescription());
    }

    @Test
    void viewerDoesNotLoadPrivateBookingViews() {
        Item item = new Item(2L, "Дрель", "Описание", true, owner, null);
        when(items.findById(2L)).thenReturn(Optional.of(item));
        when(comments.findForItems(List.of(2L))).thenReturn(List.of());
        ItemResponseDto result = service.getById(9L, 2L);
        assertNull(result.getLastBooking());
        assertNull(result.getNextBooking());
        verifyNoInteractions(bookings);
    }

    @Test
    void requiresCompletedRentalBeforeSavingComment() {
        Item item = new Item(2L, "Дрель", "Описание", true, owner, null);
        when(users.findById(1L)).thenReturn(Optional.of(owner));
        when(items.findById(2L)).thenReturn(Optional.of(item));
        assertThrows(BadRequestException.class, () -> service.addComment(1L, 2L, new CommentRequestDto("Текст")));
        verify(bookings).existsByItemIdAndBookerIdAndStatusAndEndLessThanEqual(
                2L, 1L, BookingStatus.APPROVED, LocalDateTime.now(clock));
        verifyNoInteractions(comments);
    }
}
