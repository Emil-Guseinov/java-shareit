package ru.practicum.shareit.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ru.practicum.shareit.booking.dto.BookingRequestDto;

import java.time.LocalDateTime;

public class BookingDatesValidator implements ConstraintValidator<ValidBookingDates, BookingRequestDto> {
    @Override
    public boolean isValid(BookingRequestDto dto, ConstraintValidatorContext context) {
        if (dto == null || dto.getStart() == null || dto.getEnd() == null) {
            return true;
        }
        LocalDateTime now = LocalDateTime.now(context.getClockProvider().getClock());
        return dto.getStart().isAfter(now) && dto.getEnd().isAfter(dto.getStart());
    }
}
