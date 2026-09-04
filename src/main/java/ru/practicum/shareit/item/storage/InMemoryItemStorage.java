package ru.practicum.shareit.item.storage;

import org.springframework.stereotype.Repository;
import ru.practicum.shareit.item.model.Item;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryItemStorage implements ItemStorage {
    private final Map<Long, Item> items = new LinkedHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong();

    @Override
    public synchronized Item save(Item item) {
        long id = idGenerator.incrementAndGet();
        Item savedItem = new Item(
                id,
                item.getName(),
                item.getDescription(),
                item.getAvailable(),
                item.getOwner(),
                item.getRequest()
        );

        items.put(id, savedItem);
        return savedItem;
    }

    @Override
    public synchronized Item update(Item item) {
        items.put(item.getId(), item);
        return item;
    }

    @Override
    public synchronized Optional<Item> findById(long itemId) {
        return Optional.ofNullable(items.get(itemId));
    }

    @Override
    public synchronized List<Item> findByOwnerId(long ownerId) {
        return items.values().stream()
                .filter(item -> item.getOwner().getId().equals(ownerId))
                .toList();
    }

    @Override
    public synchronized List<Item> searchAvailable(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String query = text.toLowerCase(Locale.ROOT);

        return items.values().stream()
                .filter(item -> Boolean.TRUE.equals(item.getAvailable()))
                .filter(item -> item.getName().toLowerCase(Locale.ROOT).contains(query)
                        || item.getDescription().toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }
}
