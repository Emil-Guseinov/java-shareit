package ru.practicum.shareit.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import ru.practicum.shareit.common.exception.ConflictException;
import ru.practicum.shareit.common.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.repository.UserRepository;
import ru.practicum.shareit.user.service.UserServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {
    @Mock
    private UserRepository repository;
    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(repository);
    }

    @Test
    void createIgnoresClientSuppliedIdAndUsesRepositoryResult() {
        when(repository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertNull(user.getId());
            user.setId(10L);
            return user;
        });
        UserDto result = service.create(new UserDto(999L, "Имя", "a@example.com"));
        assertEquals(10L, result.getId());
        assertEquals("a@example.com", result.getEmail());
    }

    @Test
    void duplicateEmailIsRejectedBeforeWrite() {
        when(repository.existsByEmail("a@example.com")).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.create(new UserDto(null, "Имя", "a@example.com")));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void patchKeepsOmittedEmailAndFlushesChanges() {
        User user = new User(1L, "Старое имя", "a@example.com");
        when(repository.findLockedById(1L)).thenReturn(Optional.of(user));
        UserDto result = service.update(1L, new UserDto(9L, "Новое имя", null));
        assertEquals(1L, result.getId());
        assertEquals("Новое имя", result.getName());
        assertEquals("a@example.com", result.getEmail());
        verify(repository, never()).existsByEmailAndIdNot(anyString(), anyLong());
        verify(repository).flush();
    }

    @Test
    void emailPatchRejectsDuplicateWithoutChangingEntity() {
        User user = new User(1L, "Имя", "a@example.com");
        when(repository.findLockedById(1L)).thenReturn(Optional.of(user));
        when(repository.existsByEmailAndIdNot("b@example.com", 1L)).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.update(1L, new UserDto(null, null, "b@example.com")));
        assertEquals("a@example.com", user.getEmail());
        verify(repository, never()).flush();
    }

    @Test
    void readsAndDeletesExistingUser() {
        User user = new User(1L, "Имя", "a@example.com");
        when(repository.findById(1L)).thenReturn(Optional.of(user));
        when(repository.findAll(Sort.by("id"))).thenReturn(List.of(user));
        assertEquals("Имя", service.getById(1L).getName());
        assertEquals(List.of(1L), service.getAll().stream().map(UserDto::getId).toList());
        service.delete(1L);
        verify(repository).delete(user);
        verify(repository).flush();
    }

    @Test
    void missingUserIs404ForReadUpdateAndDelete() {
        assertThrows(NotFoundException.class, () -> service.getById(1L));
        assertThrows(NotFoundException.class, () -> service.update(1L, new UserDto()));
        assertThrows(NotFoundException.class, () -> service.delete(1L));
    }
}
