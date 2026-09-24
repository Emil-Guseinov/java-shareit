package ru.practicum.shareit.booking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.support.AbstractIntegrationTest;
import ru.practicum.shareit.user.User;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookingControllerTest extends AbstractIntegrationTest {
    @Test
    void shouldCreateReadAndApproveBooking() throws Exception {
        User owner = user("Владелец");
        User booker = user("Арендатор");
        Item item = item(owner, "Дрель", true);
        BookingRequestDto request = new BookingRequestDto(item.getId(), NOW.plusDays(1), NOW.plusDays(2));
        String body = mvc.perform(post("/bookings").header(USER_HEADER, booker.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.start").value("2030-01-16T12:00:00"))
                .andExpect(jsonPath("$.end").value("2030-01-17T12:00:00"))
                .andExpect(jsonPath("$.booker.id").value(booker.getId().intValue()))
                .andExpect(jsonPath("$.booker.email").doesNotExist())
                .andExpect(jsonPath("$.item.id").value(item.getId().intValue()))
                .andExpect(jsonPath("$.item.name").value("Дрель"))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(body).get("id").asLong();
        mvc.perform(get("/bookings/{id}", id).header(USER_HEADER, owner.getId()))
                .andExpect(status().isOk());
        mvc.perform(patch("/bookings/{id}", id).header(USER_HEADER, owner.getId()).param("approved", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void shouldReturnDefaultAllLists() throws Exception {
        User owner = user("Владелец");
        User booker = user("Арендатор");
        Item item = item(owner, "Дрель", true);
        booking(item, booker, NOW.plusDays(1), NOW.plusDays(2), BookingStatus.WAITING);
        mvc.perform(get("/bookings").header(USER_HEADER, booker.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/bookings/owner").header(USER_HEADER, owner.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/bookings", "/bookings/owner"})
    void shouldRejectUnknownState(String path) throws Exception {
        mvc.perform(get(path).header(USER_HEADER, 1).param("state", "UNSUPPORTED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown state: UNSUPPORTED"));
    }

    @Test
    void shouldRejectMissingOrInvalidApprovedParameter() throws Exception {
        mvc.perform(patch("/bookings/1").header(USER_HEADER, 1))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/bookings/1").header(USER_HEADER, 1).param("approved", "maybe"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingDatesAndNonPositiveItemId() throws Exception {
        mvc.perform(post("/bookings").header(USER_HEADER, 1).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidDateFormat() throws Exception {
        mvc.perform(post("/bookings").header(USER_HEADER, 1).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":1,\"start\":\"tomorrow\",\"end\":\"later\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectNonOwnerAndReadByOutsider() throws Exception {
        Item item = item(user("Владелец"), "Дрель", true);
        Booking booking = booking(item, user("Арендатор"), NOW.plusDays(1), NOW.plusDays(2), BookingStatus.WAITING);
        mvc.perform(patch("/bookings/{id}", booking.getId()).header(USER_HEADER, Long.MAX_VALUE)
                        .param("approved", "true"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/bookings/{id}", booking.getId()).header(USER_HEADER, Long.MAX_VALUE))
                .andExpect(status().isForbidden());
    }
}
