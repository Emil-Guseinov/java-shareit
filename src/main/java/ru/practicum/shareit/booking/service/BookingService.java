package ru.practicum.shareit.booking.service;

import ru.practicum.shareit.booking.BookingState;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingRequestDto;

import java.util.List;

public interface BookingService {
    BookingDto create(long userId, BookingRequestDto request);

    BookingDto approve(long userId, long bookingId, boolean approved);

    BookingDto getById(long userId, long bookingId);

    List<BookingDto> getByBooker(long userId, BookingState state);

    List<BookingDto> getByOwner(long userId, BookingState state);
}
