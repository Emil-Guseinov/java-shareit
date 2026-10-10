package ru.practicum.shareit.item.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class BookingShortDto {
    private final Long id;
    private final Long bookerId;
    private final LocalDateTime start;
    private final LocalDateTime end;
}
