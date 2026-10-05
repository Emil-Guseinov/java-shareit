package ru.practicum.shareit.request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.common.exception.NotFoundException;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.item.repository.RequestItemView;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.request.service.ItemRequestServiceImpl;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemRequestServiceUnitTest {
    private static final LocalDateTime DATE = LocalDateTime.of(2030, 1, 15, 12, 0);

    @Mock
    private ItemRequestRepository requests;
    @Mock
    private ItemRepository items;
    @Mock
    private UserRepository users;
    private ItemRequestServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ItemRequestServiceImpl(requests, items, users);
    }

    @Test
    void assignsAuthorTimeAndGeneratedId() {
        User author = new User(2L, "Автор", "author@example.com");
        when(users.findById(2L)).thenReturn(Optional.of(author));
        when(requests.save(any(ItemRequest.class))).thenAnswer(invocation -> {
            ItemRequest request = invocation.getArgument(0);
            assertEquals(author, request.getRequester());
            request.setId(10L);
            return request;
        });
        LocalDateTime before = LocalDateTime.now();
        ItemRequestDto result = service.create(2L, new ItemRequestCreateDto("Описание"));
        LocalDateTime after = LocalDateTime.now();
        assertTrue(!result.getCreated().isBefore(before) && !result.getCreated().isAfter(after));
        assertEquals(10L, result.getId());
        assertEquals("Описание", result.getDescription());
        assertTrue(result.getItems().isEmpty());
        verifyNoInteractions(items);
    }

    @Test
    void doesNotQueryRepliesForEmptyRequestList() {
        when(users.existsById(2L)).thenReturn(true);
        when(requests.findAllByRequesterIdOrderByCreatedDescIdDesc(2L)).thenReturn(List.of());
        assertTrue(service.getOwn(2L).isEmpty());
        verifyNoInteractions(items);
    }

    @Test
    void groupsReplyProjectionsAndPreservesRequestOrdering() {
        User author = new User(2L, "Автор", "author@example.com");
        ItemRequest first = new ItemRequest(10L, "Первый", author, DATE);
        ItemRequest second = new ItemRequest(9L, "Второй", author, DATE.minusDays(1));
        when(users.existsById(2L)).thenReturn(true);
        when(requests.findAllByRequesterIdOrderByCreatedDescIdDesc(2L)).thenReturn(List.of(first, second));
        when(items.findAnswers(List.of(10L, 9L))).thenReturn(List.of(new RequestItemView(10L, 3L, "Дрель", 4L)));
        List<ItemRequestDto> result = service.getOwn(2L);
        assertEquals(List.of(10L, 9L), result.stream().map(ItemRequestDto::getId).toList());
        assertEquals(4L, result.getFirst().getItems().getFirst().getOwnerId());
        assertTrue(result.getLast().getItems().isEmpty());
    }

    @Test
    void missingUserShortCircuitsRepositories() {
        assertThrows(NotFoundException.class, () -> service.getOthers(2L));
        verifyNoInteractions(requests, items);
    }
}
