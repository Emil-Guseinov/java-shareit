package ru.practicum.shareit.booking;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.client.BaseClient;

import java.util.Map;

@Service
public class BookingClient extends BaseClient {
    public BookingClient(RestTemplate rest) {
        super(rest);
    }

    public ResponseEntity<byte[]> create(long userId, BookingRequestDto dto) {
        return post("/bookings", userId, dto);
    }

    public ResponseEntity<byte[]> approve(long userId, long id, boolean approved) {
        return patch("/bookings/" + id + "?approved={approved}", userId, Map.of("approved", approved), null);
    }

    public ResponseEntity<byte[]> getById(long userId, long id) {
        return get("/bookings/" + id, userId, Map.of());
    }

    public ResponseEntity<byte[]> getByBooker(long userId, BookingState state) {
        return get("/bookings?state={state}", userId, Map.of("state", state.name()));
    }

    public ResponseEntity<byte[]> getByOwner(long userId, BookingState state) {
        return get("/bookings/owner?state={state}", userId, Map.of("state", state.name()));
    }
}
