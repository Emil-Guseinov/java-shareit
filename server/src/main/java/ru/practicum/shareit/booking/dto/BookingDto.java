package ru.practicum.shareit.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.practicum.shareit.booking.BookingStatus;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class BookingDto {
    private final Long id;
    private final LocalDateTime start;
    private final LocalDateTime end;
    private final BookingStatus status;
    private final BookingItemDto item;
    private final BookerDto booker;
}
