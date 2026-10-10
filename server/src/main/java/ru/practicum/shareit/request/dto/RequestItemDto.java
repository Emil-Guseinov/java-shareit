package ru.practicum.shareit.request.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RequestItemDto {
    private final Long id;
    private final String name;
    private final Long ownerId;
}
