package ru.practicum.shareit.item.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CommentDto {
    private final Long id;
    private final String text;
    private final String authorName;
    private final LocalDateTime created;
}
