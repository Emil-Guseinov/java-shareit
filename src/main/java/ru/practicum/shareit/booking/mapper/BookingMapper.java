package ru.practicum.shareit.booking.mapper;

import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.dto.BookerDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingItemDto;
import ru.practicum.shareit.item.dto.BookingShortDto;

public final class BookingMapper {
    private BookingMapper() {
    }

    public static BookingDto toDto(Booking booking) {
        return new BookingDto(booking.getId(), booking.getStart(), booking.getEnd(), booking.getStatus(),
                new BookingItemDto(booking.getItem().getId(), booking.getItem().getName()),
                new BookerDto(booking.getBooker().getId()));
    }

    public static BookingShortDto toShortDto(Booking booking) {
        return new BookingShortDto(booking.getId(), booking.getBooker().getId(),
                booking.getStart(), booking.getEnd());
    }
}
