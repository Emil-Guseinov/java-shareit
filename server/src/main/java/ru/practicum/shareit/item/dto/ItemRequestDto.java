package ru.practicum.shareit.item.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemRequestDto {
    private String name;

    private String description;

    private Boolean available;

    private Long requestId;

    public ItemRequestDto(String name, String description, Boolean available) {
        this(name, description, available, null);
    }
}
