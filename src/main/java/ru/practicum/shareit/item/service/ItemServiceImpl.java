package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.common.exception.BadRequestException;
import ru.practicum.shareit.common.exception.ForbiddenException;
import ru.practicum.shareit.common.exception.NotFoundException;
import ru.practicum.shareit.item.Comment;
import ru.practicum.shareit.item.dto.BookingShortDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.CommentRequestDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemResponseDto;
import ru.practicum.shareit.item.mapper.CommentMapper;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;
    private final Clock clock;

    @Override
    @Transactional
    public ItemDto create(long userId, ItemDto dto) {
        User owner = getUserOrThrow(userId);
        return ItemMapper.toDto(itemRepository.save(ItemMapper.toModel(dto, owner)));
    }

    @Override
    @Transactional
    public ItemDto update(long userId, long itemId, ItemDto dto) {
        requireUser(userId);
        Item item = itemRepository.findLockedById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id=" + itemId + " не найдена"));
        if (!item.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Редактировать вещь может только её владелец");
        }
        if (dto.getName() != null) {
            item.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            item.setDescription(dto.getDescription());
        }
        if (dto.getAvailable() != null) {
            item.setAvailable(dto.getAvailable());
        }
        return ItemMapper.toDto(item);
    }

    @Override
    public ItemResponseDto getById(long userId, long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id=" + itemId + " не найдена"));
        return enrich(List.of(item), item.getOwner().getId().equals(userId)).getFirst();
    }

    @Override
    public List<ItemResponseDto> getByOwner(long userId) {
        requireUser(userId);
        return enrich(itemRepository.findAllByOwnerIdOrderByIdAsc(userId), true);
    }

    @Override
    public List<ItemDto> search(String text) {
        return itemRepository.searchAvailable(text).stream()
                .map(ItemMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public CommentDto addComment(long userId, long itemId, CommentRequestDto dto) {
        User author = getUserOrThrow(userId);
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id=" + itemId + " не найдена"));
        LocalDateTime now = LocalDateTime.now(clock);
        boolean hasUsedItem = bookingRepository.existsByItemIdAndBookerIdAndStatusAndEndLessThanEqual(
                itemId, userId, BookingStatus.APPROVED, now);
        if (!hasUsedItem) {
            throw new BadRequestException("Оставить отзыв можно только после завершённой аренды этой вещи");
        }
        Comment comment = new Comment(null, dto.getText(), item, author, now);
        return CommentMapper.toDto(commentRepository.save(comment));
    }

    private List<ItemResponseDto> enrich(List<Item> items, boolean ownerView) {
        if (items.isEmpty()) {
            return List.of();
        }
        List<Long> ids = items.stream().map(Item::getId).toList();
        LocalDateTime now = LocalDateTime.now(clock);
        Map<Long, BookingShortDto> last = ownerView
                ? bookingMap(bookingRepository.findLastForItems(ids, BookingStatus.APPROVED, now)) : Map.of();
        Map<Long, BookingShortDto> next = ownerView
                ? bookingMap(bookingRepository.findNextForItems(ids, BookingStatus.APPROVED, now)) : Map.of();
        Map<Long, List<CommentDto>> comments = commentRepository.findForItems(ids).stream()
                .collect(Collectors.groupingBy(comment -> comment.getItem().getId(),
                        Collectors.mapping(CommentMapper::toDto, Collectors.toList())));
        return items.stream()
                .map(item -> ItemMapper.toResponse(item, last.get(item.getId()), next.get(item.getId()),
                        comments.getOrDefault(item.getId(), List.of())))
                .toList();
    }

    private Map<Long, BookingShortDto> bookingMap(List<Booking> bookings) {
        return bookings.stream().collect(Collectors.toMap(
                booking -> booking.getItem().getId(), BookingMapper::toShortDto));
    }

    private User getUserOrThrow(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    }

    private void requireUser(long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
    }
}
