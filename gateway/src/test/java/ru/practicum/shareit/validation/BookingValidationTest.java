package ru.practicum.shareit.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.practicum.shareit.booking.BookingState;
import ru.practicum.shareit.common.exception.BadRequestException;

import static org.junit.jupiter.api.Assertions.*;

class BookingValidationTest {
    @ParameterizedTest
    @EnumSource(BookingState.class)
    void parsesAllStates(BookingState state) {
        assertEquals(state, BookingState.from(state.name()));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "all", "UNKNOWN"})
    void rejectsUnknownState(String state) {
        assertThrows(BadRequestException.class, () -> BookingState.from(state));
    }

    @Test
    void nullObjectIsLeftToNotNullConstraint() {
        assertTrue(new BookingDatesValidator().isValid(null, null));
    }
}
