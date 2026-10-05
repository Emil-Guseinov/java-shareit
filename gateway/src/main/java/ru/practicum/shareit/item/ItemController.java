package ru.practicum.shareit.item;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.item.dto.CommentRequestDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.validation.Create;
import ru.practicum.shareit.validation.Update;

@RestController
@Validated
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";
    private final ItemClient itemClient;

    @PostMapping
    public ResponseEntity<byte[]> create(@RequestHeader(USER_ID_HEADER) @Positive long userId,
                                         @Validated(Create.class) @RequestBody ItemDto itemDto) {
        return itemClient.create(userId, itemDto);
    }

    @PatchMapping("/{itemId}")
    public ResponseEntity<byte[]> update(@RequestHeader(USER_ID_HEADER) @Positive long userId,
                                         @PathVariable @Positive long itemId,
                                         @Validated(Update.class) @RequestBody ItemDto itemDto) {
        return itemClient.update(userId, itemId, itemDto);
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<byte[]> getById(@RequestHeader(USER_ID_HEADER) @Positive long userId,
                                          @PathVariable @Positive long itemId) {
        return itemClient.getById(userId, itemId);
    }

    @GetMapping
    public ResponseEntity<byte[]> getByOwner(@RequestHeader(USER_ID_HEADER) @Positive long userId) {
        return itemClient.getByOwner(userId);
    }

    @GetMapping("/search")
    public ResponseEntity<byte[]> search(@RequestHeader(USER_ID_HEADER) @Positive long userId,
                                         @RequestParam String text) {
        if (text.isBlank()) {
            return ResponseEntity.ok().contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body("[]".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        return itemClient.search(userId, text);
    }

    @PostMapping("/{itemId}/comment")
    public ResponseEntity<byte[]> addComment(@RequestHeader(USER_ID_HEADER) @Positive long userId,
                                             @PathVariable @Positive long itemId,
                                             @Valid @RequestBody CommentRequestDto commentDto) {
        return itemClient.addComment(userId, itemId, commentDto);
    }
}
