package ru.practicum.shareit.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import ru.practicum.shareit.validation.Create;
import ru.practicum.shareit.validation.Update;

public class ItemDto {
    private Long id;

    @NotBlank(groups = Create.class)
    @Pattern(regexp = "(?s).*\\S.*", groups = Update.class)
    private String name;

    @NotBlank(groups = Create.class)
    @Pattern(regexp = "(?s).*\\S.*", groups = Update.class)
    private String description;

    @NotNull(groups = Create.class)
    private Boolean available;

    public ItemDto() {
    }

    public ItemDto(Long id, String name, String description, Boolean available) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.available = available;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }
}
