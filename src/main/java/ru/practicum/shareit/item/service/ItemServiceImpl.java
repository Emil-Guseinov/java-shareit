package ru.practicum.shareit.item.service;

import org.springframework.stereotype.Service;
import ru.practicum.shareit.common.exception.ForbiddenException;
import ru.practicum.shareit.common.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.storage.ItemStorage;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.storage.UserStorage;

import java.util.List;

@Service
public class ItemServiceImpl implements ItemService {
    private final ItemStorage itemStorage;
    private final UserStorage userStorage;

    public ItemServiceImpl(ItemStorage itemStorage, UserStorage userStorage) {
        this.itemStorage = itemStorage;
        this.userStorage = userStorage;
    }

    @Override
    public ItemDto create(long userId, ItemDto itemDto) {
        User owner = getUserOrThrow(userId);
        Item savedItem = itemStorage.save(ItemMapper.toModel(itemDto, owner));

        return ItemMapper.toDto(savedItem);
    }

    @Override
    public ItemDto update(long userId, long itemId, ItemDto itemDto) {
        getUserOrThrow(userId);
        Item current = getItemOrThrow(itemId);

        if (!current.getOwner().getId().equals(userId)) {
            throw new ForbiddenException(
                    "Редактировать вещь может только её владелец");
        }

        Item updated = new Item(
                current.getId(),
                itemDto.getName() != null ? itemDto.getName() : current.getName(),
                itemDto.getDescription() != null
                        ? itemDto.getDescription() : current.getDescription(),
                itemDto.getAvailable() != null ? itemDto.getAvailable() : current.getAvailable(),
                current.getOwner(),
                current.getRequest()
        );

        return ItemMapper.toDto(itemStorage.update(updated));
    }

    @Override
    public ItemDto getById(long itemId) {
        return ItemMapper.toDto(getItemOrThrow(itemId));
    }

    @Override
    public List<ItemDto> getByOwner(long userId) {
        getUserOrThrow(userId);
        return itemStorage.findByOwnerId(userId).stream()
                .map(ItemMapper::toDto)
                .toList();
    }

    @Override
    public List<ItemDto> search(String text) {
        return itemStorage.searchAvailable(text).stream()
                .map(ItemMapper::toDto)
                .toList();
    }

    private Item getItemOrThrow(long itemId) {
        return itemStorage.findById(itemId)
                .orElseThrow(() -> new NotFoundException(
                        "Вещь с id=" + itemId + " не найдена"));
    }

    private User getUserOrThrow(long userId) {
        return userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        "Пользователь с id=" + userId + " не найден"));
    }
}
