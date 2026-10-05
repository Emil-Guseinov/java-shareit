package ru.practicum.shareit.request.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.common.exception.NotFoundException;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.item.repository.RequestItemView;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.RequestItemDto;
import ru.practicum.shareit.request.mapper.ItemRequestMapper;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemRequestServiceImpl implements ItemRequestService {
    private final ItemRequestRepository requestRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ItemRequestDto create(long userId, ItemRequestCreateDto dto) {
        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
        ItemRequest request = new ItemRequest(null, dto.getDescription(), requester, LocalDateTime.now());
        return ItemRequestMapper.toDto(requestRepository.save(request), List.of());
    }

    @Override
    public List<ItemRequestDto> getOwn(long userId) {
        requireUser(userId);
        return withAnswers(requestRepository.findAllByRequesterIdOrderByCreatedDescIdDesc(userId));
    }

    @Override
    public List<ItemRequestDto> getOthers(long userId) {
        requireUser(userId);
        return withAnswers(requestRepository.findAllByRequesterIdNotOrderByCreatedDescIdDesc(userId));
    }

    @Override
    public ItemRequestDto getById(long userId, long requestId) {
        requireUser(userId);
        ItemRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос с id=" + requestId + " не найден"));
        return withAnswers(List.of(request)).getFirst();
    }

    private List<ItemRequestDto> withAnswers(List<ItemRequest> requests) {
        if (requests.isEmpty()) {
            return List.of();
        }
        List<Long> ids = requests.stream().map(ItemRequest::getId).toList();
        Map<Long, List<RequestItemDto>> answers = itemRepository.findAnswers(ids).stream()
                .collect(Collectors.groupingBy(RequestItemView::requestId,
                        Collectors.mapping(item -> new RequestItemDto(item.id(), item.name(), item.ownerId()),
                                Collectors.toList())));
        return requests.stream()
                .map(request -> ItemRequestMapper.toDto(request, answers.getOrDefault(request.getId(), List.of())))
                .toList();
    }

    private void requireUser(long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
    }
}
