package ru.practicum.shareit.request.dto;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class ItemRequestDto {
    private final Long id;
    private final String description;
    private final LocalDateTime created;
    private final List<RequestItemDto> items;

    public ItemRequestDto(Long id, String description, LocalDateTime created, List<RequestItemDto> items) {
        this.id = id;
        this.description = description;
        this.created = created;
        this.items = List.copyOf(items);
    }
}
