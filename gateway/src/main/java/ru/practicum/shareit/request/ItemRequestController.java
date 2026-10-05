package ru.practicum.shareit.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;

@RestController
@Validated
@RequestMapping("/requests")
@RequiredArgsConstructor
public class ItemRequestController {
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";
    private final ItemRequestClient requestClient;

    @PostMapping
    public ResponseEntity<Object> create(@RequestHeader(USER_ID_HEADER) @Positive long userId,
                                         @Valid @RequestBody ItemRequestCreateDto dto) {
        return requestClient.create(userId, dto);
    }

    @GetMapping
    public ResponseEntity<Object> getOwn(@RequestHeader(USER_ID_HEADER) @Positive long userId) {
        return requestClient.getOwn(userId);
    }

    @GetMapping("/all")
    public ResponseEntity<Object> getOthers(@RequestHeader(USER_ID_HEADER) @Positive long userId) {
        return requestClient.getOthers(userId);
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<Object> getById(@RequestHeader(USER_ID_HEADER) @Positive long userId, @PathVariable @Positive long requestId) {
        return requestClient.getById(userId, requestId);
    }
}
