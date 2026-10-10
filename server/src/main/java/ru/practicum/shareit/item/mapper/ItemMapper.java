package ru.practicum.shareit.item.mapper;

import ru.practicum.shareit.item.dto.BookingShortDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemRequestDto;
import ru.practicum.shareit.item.dto.ItemResponseDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;

import java.util.List;

public final class ItemMapper {
    private ItemMapper() {
    }

    public static ItemDto toDto(Item item) {
        return new ItemDto(item.getId(), item.getName(), item.getDescription(), item.getAvailable(),
                item.getRequest() == null ? null : item.getRequest().getId());
    }

    public static Item toModel(ItemRequestDto dto, User owner) {
        return new Item(null, dto.getName(), dto.getDescription(), dto.getAvailable(), owner, null);
    }

    public static ItemResponseDto toResponse(Item item, BookingShortDto last, BookingShortDto next,
                                             List<CommentDto> comments) {
        return new ItemResponseDto(item.getId(), item.getName(), item.getDescription(),
                item.getAvailable(), last, next, comments,
                item.getRequest() == null ? null : item.getRequest().getId());
    }
}
