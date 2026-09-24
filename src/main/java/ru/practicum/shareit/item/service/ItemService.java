package ru.practicum.shareit.item.service;

import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.CommentRequestDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemResponseDto;

import java.util.List;

public interface ItemService {
    ItemDto create(long userId, ItemDto itemDto);

    ItemDto update(long userId, long itemId, ItemDto itemDto);

    ItemResponseDto getById(long userId, long itemId);

    List<ItemResponseDto> getByOwner(long userId);

    List<ItemDto> search(String text);

    CommentDto addComment(long userId, long itemId, CommentRequestDto commentDto);
}
