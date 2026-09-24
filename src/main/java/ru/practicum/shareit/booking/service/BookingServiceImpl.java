package ru.practicum.shareit.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingState;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.common.exception.BadRequestException;
import ru.practicum.shareit.common.exception.ForbiddenException;
import ru.practicum.shareit.common.exception.NotFoundException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Override
    @Transactional
    public BookingDto create(long userId, BookingRequestDto request) {
        validateDates(request);
        User booker = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
        Item item = itemRepository.findById(request.getItemId())
                .orElseThrow(() -> new NotFoundException("Вещь с id=" + request.getItemId() + " не найдена"));
        if (item.getOwner().getId().equals(userId)) {
            throw new BadRequestException("Нельзя бронировать собственную вещь");
        }
        if (!Boolean.TRUE.equals(item.getAvailable())) {
            throw new BadRequestException("Вещь недоступна для аренды");
        }
        Booking booking = new Booking(null, request.getStart(), request.getEnd(), item, booker,
                BookingStatus.WAITING);
        return BookingMapper.toDto(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingDto approve(long userId, long bookingId, boolean approved) {
        // До блокировки читаем только id, чтобы не загрузить устаревшее состояние бронирования.
        long itemId = bookingRepository.findItemId(bookingId)
                .orElseThrow(() -> new NotFoundException("Бронирование с id=" + bookingId + " не найдено"));
        Item item = itemRepository.findLockedById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id=" + itemId + " не найдена"));
        if (!item.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Подтвердить или отклонить бронирование может только владелец вещи");
        }
        Booking booking = getBookingOrThrow(bookingId);
        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new BadRequestException("Решение по бронированию уже принято");
        }
        if (approved) {
            if (!Boolean.TRUE.equals(item.getAvailable())) {
                throw new BadRequestException("Вещь недоступна для аренды");
            }
            if (bookingRepository.existsOverlapping(itemId, bookingId, BookingStatus.APPROVED,
                    booking.getStart(), booking.getEnd())) {
                throw new BadRequestException("Вещь уже забронирована на пересекающиеся даты");
            }
        }
        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        // Сохраняем решение до освобождения блокировки вещи в конце транзакции.
        bookingRepository.flush();
        return BookingMapper.toDto(booking);
    }

    @Override
    public BookingDto getById(long userId, long bookingId) {
        Booking booking = getBookingOrThrow(bookingId);
        if (!booking.getBooker().getId().equals(userId)
                && !booking.getItem().getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Бронирование доступно только арендатору и владельцу вещи");
        }
        return BookingMapper.toDto(booking);
    }

    @Override
    public List<BookingDto> getByBooker(long userId, BookingState state) {
        requireUser(userId);
        return bookingRepository.findForBooker(userId, state.name(), LocalDateTime.now(clock),
                        BookingStatus.WAITING, BookingStatus.REJECTED).stream()
                .map(BookingMapper::toDto)
                .toList();
    }

    @Override
    public List<BookingDto> getByOwner(long userId, BookingState state) {
        requireUser(userId);
        return bookingRepository.findForOwner(userId, state.name(), LocalDateTime.now(clock),
                        BookingStatus.WAITING, BookingStatus.REJECTED).stream()
                .map(BookingMapper::toDto)
                .toList();
    }

    private void validateDates(BookingRequestDto request) {
        LocalDateTime start = request.getStart();
        LocalDateTime end = request.getEnd();
        if (start == null || end == null || !end.isAfter(start)) {
            throw new BadRequestException("Конец бронирования должен быть позже начала");
        }
        if (!start.isAfter(LocalDateTime.now(clock))) {
            throw new BadRequestException("Начало бронирования должно быть в будущем");
        }
    }

    private Booking getBookingOrThrow(long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Бронирование с id=" + bookingId + " не найдено"));
    }

    private void requireUser(long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
    }
}
