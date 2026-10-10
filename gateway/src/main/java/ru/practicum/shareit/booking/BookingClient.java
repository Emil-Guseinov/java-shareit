package ru.practicum.shareit.booking;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.client.BaseClient;

import java.util.Map;

@Service
public class BookingClient extends BaseClient {
    private static final String API_PREFIX = "/bookings";

    public BookingClient(RestTemplate rest) {
        super(rest);
    }

    public ResponseEntity<Object> create(long userId, BookingRequestDto dto) {
        return post(API_PREFIX, userId, dto);
    }

    public ResponseEntity<Object> approve(long userId, long id, boolean approved) {
        return patch(API_PREFIX + "/" + id + "?approved={approved}", userId, Map.of("approved", approved), null);
    }

    public ResponseEntity<Object> getById(long userId, long id) {
        return get(API_PREFIX + "/" + id, userId, Map.of());
    }

    public ResponseEntity<Object> getByBooker(long userId, BookingState state) {
        return get(API_PREFIX + "?state={state}", userId, Map.of("state", state.name()));
    }

    public ResponseEntity<Object> getByOwner(long userId, BookingState state) {
        return get(API_PREFIX + "/owner?state={state}", userId, Map.of("state", state.name()));
    }
}
