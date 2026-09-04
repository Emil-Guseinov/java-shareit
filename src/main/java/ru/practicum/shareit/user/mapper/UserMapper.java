package ru.practicum.shareit.user.mapper;

import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.dto.UserDto;

public final class UserMapper {
    private UserMapper() {
    }

    public static UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getName(), user.getEmail());
    }

    public static User toModel(UserDto userDto) {
        return new User(userDto.getId(), userDto.getName(), userDto.getEmail());
    }
}
