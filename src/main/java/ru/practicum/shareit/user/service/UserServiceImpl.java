package ru.practicum.shareit.user.service;

import org.springframework.stereotype.Service;
import ru.practicum.shareit.common.exception.ConflictException;
import ru.practicum.shareit.common.exception.NotFoundException;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.storage.UserStorage;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    private final UserStorage userStorage;

    public UserServiceImpl(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    @Override
    public UserDto create(UserDto userDto) {
        ensureEmailIsUnique(userDto.getEmail(), null);
        User savedUser = userStorage.save(UserMapper.toModel(userDto));

        return UserMapper.toDto(savedUser);
    }

    @Override
    public UserDto update(long userId, UserDto userDto) {
        User current = getUserOrThrow(userId);
        String email = userDto.getEmail() != null ? userDto.getEmail() : current.getEmail();
        String name = userDto.getName() != null ? userDto.getName() : current.getName();

        if (userDto.getEmail() != null) {
            ensureEmailIsUnique(email, userId);
        }

        User updated = new User(current.getId(), name, email);

        return UserMapper.toDto(userStorage.update(updated));
    }

    @Override
    public UserDto getById(long userId) {
        return UserMapper.toDto(getUserOrThrow(userId));
    }

    @Override
    public List<UserDto> getAll() {
        return userStorage.findAll().stream()
                .map(UserMapper::toDto)
                .toList();
    }

    @Override
    public void delete(long userId) {
        getUserOrThrow(userId);
        userStorage.delete(userId);
    }

    private User getUserOrThrow(long userId) {
        return userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        "Пользователь с id=" + userId + " не найден"));
    }

    private void ensureEmailIsUnique(String email, Long currentUserId) {
        userStorage.findByEmail(email)
                .filter(user -> currentUserId == null || !user.getId().equals(currentUserId))
                .ifPresent(user -> {
                    throw new ConflictException(
                            "Пользователь с email=" + email + " уже существует");
                });
    }
}
