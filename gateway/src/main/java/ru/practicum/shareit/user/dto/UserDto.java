package ru.practicum.shareit.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class UserDto {
    private Long id;

    @NotBlank(groups = Create.class)
    @Size(max = 255, groups = {Create.class, Update.class})
    private String name;

    @NotBlank(groups = Create.class)
    @Email(groups = {Create.class, Update.class})
    @Size(min = 1, max = 512, groups = {Create.class, Update.class})
    private String email;
}
