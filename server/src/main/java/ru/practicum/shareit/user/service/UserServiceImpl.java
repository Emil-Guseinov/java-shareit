package ru.practicum.shareit.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.common.exception.ConflictException;
import ru.practicum.shareit.common.exception.NotFoundException;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDto create(UserDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ConflictException("Пользователь с таким email уже существует");
        }
        return UserMapper.toDto(userRepository.saveAndFlush(UserMapper.toModel(dto)));
    }

    @Override
    @Transactional
    public UserDto update(long userId, UserDto dto) {
        User user = userRepository.findLockedById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
        if (dto.getEmail() != null) {
            if (userRepository.existsByEmailAndIdNot(dto.getEmail(), userId)) {
                throw new ConflictException("Пользователь с таким email уже существует");
            }
            user.setEmail(dto.getEmail());
        }
        if (dto.getName() != null) {
            user.setName(dto.getName());
        }
        userRepository.flush();
        return UserMapper.toDto(user);
    }

    @Override
    public UserDto getById(long userId) {
        return UserMapper.toDto(getUserOrThrow(userId));
    }

    @Override
    public List<UserDto> getAll() {
        return userRepository.findAll(Sort.by("id")).stream()
                .map(UserMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void delete(long userId) {
        userRepository.delete(getUserOrThrow(userId));
        userRepository.flush();
    }

    private User getUserOrThrow(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    }
}
