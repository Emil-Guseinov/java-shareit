package ru.practicum.shareit.user.storage;

import ru.practicum.shareit.user.User;

import java.util.List;
import java.util.Optional;

public interface UserStorage {
    User save(User user);

    User update(User user);

    Optional<User> findById(long userId);

    List<User> findAll();

    Optional<User> findByEmail(String email);

    void delete(long userId);
}
