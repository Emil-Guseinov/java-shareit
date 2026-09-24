package ru.practicum.shareit.item.model;

import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.user.User;

public class Item {
    private final Long id;
    private final String name;
    private final String description;
    private final Boolean available;
    private final User owner;
    private final ItemRequest request;

    public Item(Long id, String name, String description, Boolean available,
                User owner, ItemRequest request) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.available = available;
        this.owner = owner;
        this.request = request;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getAvailable() {
        return available;
    }

    public User getOwner() {
        return owner;
    }

    public ItemRequest getRequest() {
        return request;
    }
}
