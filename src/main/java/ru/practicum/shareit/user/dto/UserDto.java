package ru.practicum.shareit.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import ru.practicum.shareit.validation.Create;
import ru.practicum.shareit.validation.Update;

public class UserDto {
    private Long id;
    private String name;

    @NotBlank(groups = Create.class)
    @Email(groups = {Create.class, Update.class})
    @Pattern(regexp = "(?s).*\\S.*", groups = Update.class)
    private String email;

    public UserDto() {
    }

    public UserDto(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
