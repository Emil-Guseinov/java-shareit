package ru.practicum.shareit.booking;

import ru.practicum.shareit.common.exception.BadRequestException;

public enum BookingState {
    ALL,
    CURRENT,
    PAST,
    FUTURE,
    WAITING,
    REJECTED;

    public static BookingState from(String value) {
        try {
            return BookingState.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new BadRequestException("Unknown state: " + value);
        }
    }
}
