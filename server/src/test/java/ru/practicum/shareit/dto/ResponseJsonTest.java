package ru.practicum.shareit.dto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookerDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingItemDto;
import ru.practicum.shareit.item.dto.BookingShortDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemResponseDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.RequestItemDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JsonTest
class ResponseJsonTest {
    private static final LocalDateTime DATE = LocalDateTime.of(2030, 1, 15, 12, 0);
    @Autowired
    private JacksonTester<BookingDto> bookings;
    @Autowired
    private JacksonTester<ItemRequestDto> requests;
    @Autowired
    private JacksonTester<ItemResponseDto> items;

    @Test
    void bookingSerializesIsoDatesStatusAndMinimalNestedObjects() throws Exception {
        BookingDto booking = new BookingDto(1L, DATE, DATE.plusDays(1), BookingStatus.APPROVED,
                new BookingItemDto(2L, "Дрель"), new BookerDto(3L));
        assertThat(bookings.write(booking)).extractingJsonPathStringValue("$.start").isEqualTo("2030-01-15T12:00:00");
        assertThat(bookings.write(booking)).extractingJsonPathStringValue("$.end").isEqualTo("2030-01-16T12:00:00");
        assertThat(bookings.write(booking)).extractingJsonPathStringValue("$.status").isEqualTo("APPROVED");
        assertThat(bookings.write(booking)).extractingJsonPathNumberValue("$.booker.id").isEqualTo(3);
        assertThat(bookings.write(booking)).doesNotHaveJsonPath("$.booker.email");
    }

    @Test
    void requestJsonUsesOwnerIdAndEmptyArrayNotNull() throws Exception {
        ItemRequestDto request = new ItemRequestDto(1L, "Нужна дрель", DATE, List.of(new RequestItemDto(2L, "Дрель", 3L)));
        assertThat(requests.write(request)).extractingJsonPathNumberValue("$.items[0].ownerId").isEqualTo(3);
        assertThat(requests.write(request)).extractingJsonPathStringValue("$.items[0].name").isEqualTo("Дрель");
        assertThat(requests.write(request)).extractingJsonPathStringValue("$.created").isEqualTo("2030-01-15T12:00:00");
        assertThat(requests.write(request)).doesNotHaveJsonPath("$.requester.email");
        ItemRequestDto empty = new ItemRequestDto(4L, "Другой", DATE, List.of());
        assertThat(requests.write(empty)).extractingJsonPathArrayValue("$.items").isEmpty();
    }

    @Test
    void responseListsAreDefensiveCopies() {
        List<RequestItemDto> replies = new ArrayList<>();
        replies.add(new RequestItemDto(2L, "Дрель", 3L));
        ItemRequestDto request = new ItemRequestDto(1L, "Запрос", DATE, replies);
        replies.clear();
        assertEquals(1, request.getItems().size());
        assertThrows(UnsupportedOperationException.class, () -> request.getItems().clear());
        List<CommentDto> comments = new ArrayList<>();
        comments.add(new CommentDto(1L, "Отзыв", "Автор", DATE));
        ItemResponseDto item = new ItemResponseDto(1L, "Вещь", "Описание", true, null, null, comments, 2L);
        comments.clear();
        assertEquals(1, item.getComments().size());
        assertThrows(UnsupportedOperationException.class, () -> item.getComments().clear());
        assertTrue(new ItemResponseDto(2L, "Вещь", "Описание", true, null, null, List.of(), null)
                .getComments().isEmpty());
    }

    @Test
    void itemBookingViewsAndCommentsHaveStableJson() throws Exception {
        BookingShortDto last = new BookingShortDto(5L, 6L, DATE.minusDays(2), DATE.minusDays(1));
        ItemResponseDto item = new ItemResponseDto(2L, "Дрель", "Описание", true, last, null,
                List.of(new CommentDto(7L, "Отзыв", "Имя", DATE)), 8L);
        assertThat(items.write(item)).extractingJsonPathNumberValue("$.requestId").isEqualTo(8);
        assertThat(items.write(item)).extractingJsonPathNumberValue("$.lastBooking.bookerId").isEqualTo(6);
        assertThat(items.write(item)).extractingJsonPathStringValue("$.comments[0].authorName").isEqualTo("Имя");
        assertThat(items.write(item)).doesNotHaveJsonPath("$.owner");
    }
}
