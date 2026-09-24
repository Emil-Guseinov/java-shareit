package ru.practicum.shareit.item.storage;

import ru.practicum.shareit.item.model.Item;

import java.util.List;
import java.util.Optional;

public interface ItemStorage {
    Item save(Item item);

    Item update(Item item);

    Optional<Item> findById(long itemId);

    List<Item> findByOwnerId(long ownerId);

    List<Item> searchAvailable(String text);
}
