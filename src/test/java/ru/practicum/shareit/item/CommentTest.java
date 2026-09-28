package ru.practicum.shareit.item;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.common.exception.BadRequestException;
import ru.practicum.shareit.common.exception.NotFoundException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.CommentRequestDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.support.AbstractIntegrationTest;
import ru.practicum.shareit.user.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommentTest extends AbstractIntegrationTest {
    @Autowired
    private ItemService service;

    @Test
    void shouldCreateCommentAfterCompletedApprovedBooking() {
        User owner = user("Владелец");
        User author = user("Арендатор");
        Item item = item(owner, "Дрель", true);
        booking(item, author, NOW.minusDays(1), NOW, BookingStatus.APPROVED);
        CommentDto result = service.addComment(author.getId(), item.getId(), new CommentRequestDto("Отличная дрель"));
        assertEquals("Отличная дрель", result.getText());
        assertEquals(author.getName(), result.getAuthorName());
        assertEquals(NOW, result.getCreated());
        assertTrue(result.getId() > 0);
    }

    @ParameterizedTest
    @EnumSource(value = BookingStatus.class, names = {"WAITING", "REJECTED"})
    void shouldRejectCommentWithoutApprovedBooking(BookingStatus status) {
        User author = user("Арендатор");
        Item item = item(user("Владелец"), "Дрель", true);
        booking(item, author, NOW.minusDays(2), NOW.minusDays(1), status);
        assertThrows(BadRequestException.class,
                () -> service.addComment(author.getId(), item.getId(), new CommentRequestDto("Отзыв")));
    }

    @Test
    void shouldRejectCommentBeforeBookingHasEnded() {
        User author = user("Арендатор");
        Item item = item(user("Владелец"), "Дрель", true);
        booking(item, author, NOW.minusHours(1), NOW.plusHours(1), BookingStatus.APPROVED);
        assertThrows(BadRequestException.class,
                () -> service.addComment(author.getId(), item.getId(), new CommentRequestDto("Отзыв")));
    }

    @Test
    void shouldRejectCommentForAnotherItemOrAnotherBooker() {
        User author = user("Арендатор");
        Item item = item(user("Владелец"), "Дрель", true);
        Item other = item(item.getOwner(), "Пила", true);
        booking(other, author, NOW.minusDays(2), NOW.minusDays(1), BookingStatus.APPROVED);
        booking(item, user("Другой"), NOW.minusDays(2), NOW.minusDays(1), BookingStatus.APPROVED);
        assertThrows(BadRequestException.class,
                () -> service.addComment(author.getId(), item.getId(), new CommentRequestDto("Отзыв")));
    }

    @Test
    void shouldRejectMissingAuthor() {
        Item item = item(user("Владелец"), "Дрель", true);
        assertThrows(NotFoundException.class,
                () -> service.addComment(Long.MAX_VALUE, item.getId(), new CommentRequestDto("Отзыв")));
    }

    @Test
    void shouldRejectMissingItem() {
        User author = user("Арендатор");
        assertThrows(NotFoundException.class,
                () -> service.addComment(author.getId(), Long.MAX_VALUE, new CommentRequestDto("Отзыв")));
    }

    @Test
    void shouldShowCommentsInItemDetailsAndOwnerList() throws Exception {
        User owner = user("Владелец");
        User author = user("Арендатор");
        Item item = item(owner, "Дрель", true);
        booking(item, author, NOW.minusDays(2), NOW.minusDays(1), BookingStatus.APPROVED);
        mvc.perform(post("/items/{id}/comment", item.getId()).header(USER_HEADER, author.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Отзыв\",\"authorName\":\"Подмена\",\"id\":999,\"created\":\"2000-01-01T00:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorName").value("Арендатор"))
                .andExpect(jsonPath("$.created").value("2030-01-15T12:00:00"));
        mvc.perform(get("/items/{id}", item.getId()).header(USER_HEADER, author.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments[0].text").value("Отзыв"))
                .andExpect(jsonPath("$.lastBooking").isEmpty());
        mvc.perform(get("/items").header(USER_HEADER, owner.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].comments[0].authorName").value("Арендатор"))
                .andExpect(jsonPath("$[0].lastBooking.id").isNumber());
    }

    @Test
    void shouldRejectBlankCommentOnHttpBoundary() throws Exception {
        User author = user("Арендатор");
        Item item = item(user("Владелец"), "Дрель", true);
        mvc.perform(post("/items/{id}/comment", item.getId()).header(USER_HEADER, author.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }
}
