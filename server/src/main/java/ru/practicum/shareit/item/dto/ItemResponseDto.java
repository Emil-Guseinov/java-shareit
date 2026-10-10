package ru.practicum.shareit.item.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class ItemResponseDto {
    private final Long id;
    private final String name;
    private final String description;
    private final Boolean available;
    private final BookingShortDto lastBooking;
    private final BookingShortDto nextBooking;
    private final List<CommentDto> comments;
    private final Long requestId;

    public ItemResponseDto(Long id, String name, String description, Boolean available,
                           BookingShortDto lastBooking, BookingShortDto nextBooking,
                           List<CommentDto> comments, Long requestId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.available = available;
        this.lastBooking = lastBooking;
        this.nextBooking = nextBooking;
        this.comments = List.copyOf(comments);
        this.requestId = requestId;
    }
}
