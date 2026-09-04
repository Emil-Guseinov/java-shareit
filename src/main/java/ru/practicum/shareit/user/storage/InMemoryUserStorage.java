package ru.practicum.shareit.user.storage;

import org.springframework.stereotype.Repository;
import ru.practicum.shareit.user.User;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new LinkedHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong();

    @Override
    public synchronized User save(User user) {
        long id = idGenerator.incrementAndGet();
        User savedUser = new User(id, user.getName(), user.getEmail());

        users.put(id, savedUser);
        return savedUser;
    }

    @Override
    public synchronized User update(User user) {
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public synchronized Optional<User> findById(long userId) {
        return Optional.ofNullable(users.get(userId));
    }

    @Override
    public synchronized List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public synchronized Optional<User> findByEmail(String email) {
        return users.values().stream()
                .filter(user -> user.getEmail().equals(email))
                .findFirst();
    }

    @Override
    public synchronized void delete(long userId) {
        users.remove(userId);
    }
}
