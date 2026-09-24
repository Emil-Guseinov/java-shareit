package ru.practicum.shareit.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import ru.practicum.shareit.validation.Create;
import ru.practicum.shareit.validation.Update;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
}