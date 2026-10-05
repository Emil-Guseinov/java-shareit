package ru.practicum.shareit.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.booking.dto.BookingRequestDto;

@RestController
@Validated
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";
    private final BookingClient bookingClient;

    @PostMapping
    public ResponseEntity<byte[]> create(@RequestHeader(USER_ID_HEADER) @Positive long userId,
                                         @Valid @RequestBody BookingRequestDto request) {
        return bookingClient.create(userId, request);
    }

    @PatchMapping("/{bookingId}")
    public ResponseEntity<byte[]> approve(@RequestHeader(USER_ID_HEADER) @Positive long userId,
                                          @PathVariable @Positive long bookingId, @RequestParam boolean approved) {
        return bookingClient.approve(userId, bookingId, approved);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<byte[]> getById(@RequestHeader(USER_ID_HEADER) @Positive long userId,
                                          @PathVariable @Positive long bookingId) {
        return bookingClient.getById(userId, bookingId);
    }

    @GetMapping
    public ResponseEntity<byte[]> getByBooker(@RequestHeader(USER_ID_HEADER) @Positive long userId,
                                              @RequestParam(defaultValue = "ALL") String state) {
        return bookingClient.getByBooker(userId, BookingState.from(state));
    }

    @GetMapping("/owner")
    public ResponseEntity<byte[]> getByOwner(@RequestHeader(USER_ID_HEADER) @Positive long userId,
                                             @RequestParam(defaultValue = "ALL") String state) {
        return bookingClient.getByOwner(userId, BookingState.from(state));
    }
}
