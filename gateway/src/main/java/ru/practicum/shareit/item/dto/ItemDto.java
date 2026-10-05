package ru.practicum.shareit.item.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.practicum.shareit.validation.Create;
import ru.practicum.shareit.validation.Update;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemDto {
    private Long id;

    @NotBlank(groups = Create.class)
    @Pattern(regexp = "(?s).*\\S.*", groups = Update.class)
    @Size(max = 255, groups = {Create.class, Update.class})
    private String name;

    @NotBlank(groups = Create.class)
    @Pattern(regexp = "(?s).*\\S.*", groups = Update.class)
    @Size(max = 2000, groups = {Create.class, Update.class})
    private String description;

    @NotNull(groups = Create.class)
    private Boolean available;

    @Positive(groups = {Create.class, Update.class})
    private Long requestId;

    public ItemDto(Long id, String name, String description, Boolean available) {
        this(id, name, description, available, null);
    }
}
