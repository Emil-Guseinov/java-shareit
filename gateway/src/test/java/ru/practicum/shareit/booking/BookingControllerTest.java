package ru.practicum.shareit.booking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MvcResult;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.support.ControllerTestSupport;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
class BookingControllerTest extends ControllerTestSupport {
    @MockBean
    private BookingClient service;

    @Test
    void createForwardsBooking() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        BookingRequestDto request = new BookingRequestDto(2L, start, start.plusDays(1));
        Map<String, Object> expected = Map.of("id", 5L, "status", "WAITING");
        when(service.create(eq(1L), any(BookingRequestDto.class))).thenReturn(ResponseEntity.ok(expected));
        MvcResult result = mvc.perform(post("/bookings").header(HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isOk()).andReturn();
        assertBody(result, expected);
        verify(service).create(eq(1L), refEq(request));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void approveForwardsDecision(boolean approved) throws Exception {
        Map<String, Object> expected = Map.of("id", 5L, "status", approved ? "APPROVED" : "REJECTED");
        when(service.approve(1L, 5L, approved)).thenReturn(ResponseEntity.ok(expected));
        MvcResult result = mvc.perform(patch("/bookings/5").header(HEADER, 1).param("approved", String.valueOf(approved)))
                .andExpect(status().isOk()).andReturn();
        assertBody(result, expected);
        verify(service).approve(1L, 5L, approved);
    }

    @Test
    void getByIdReturnsBooking() throws Exception {
        Map<String, Object> expected = Map.of("id", 5L, "status", "WAITING");
        when(service.getById(1L, 5L)).thenReturn(ResponseEntity.ok(expected));
        MvcResult result = mvc.perform(get("/bookings/5").header(HEADER, 1)).andExpect(status().isOk()).andReturn();
        assertBody(result, expected);
        verify(service).getById(1L, 5L);
    }

    @Test
    void getByBookerUsesDefaultState() throws Exception {
        when(service.getByBooker(1L, BookingState.ALL)).thenReturn(ResponseEntity.ok(List.of()));
        MvcResult result = mvc.perform(get("/bookings").header(HEADER, 1)).andExpect(status().isOk()).andReturn();
        assertBody(result, List.of());
        verify(service).getByBooker(1L, BookingState.ALL);
    }

    @ParameterizedTest
    @EnumSource(BookingState.class)
    void getByBookerForwardsState(BookingState state) throws Exception {
        when(service.getByBooker(1L, state)).thenReturn(ResponseEntity.ok(List.of()));
        MvcResult result = mvc.perform(get("/bookings").header(HEADER, 1).param("state", state.name()))
                .andExpect(status().isOk()).andReturn();
        assertBody(result, List.of());
        verify(service).getByBooker(1L, state);
    }

    @Test
    void getByBookerRejectsUnknownState() throws Exception {
        mvc.perform(get("/bookings").header(HEADER, 1).param("state", "UNKNOWN"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void getByOwnerUsesDefaultState() throws Exception {
        when(service.getByOwner(1L, BookingState.ALL)).thenReturn(ResponseEntity.ok(List.of()));
        MvcResult result = mvc.perform(get("/bookings/owner").header(HEADER, 1)).andExpect(status().isOk()).andReturn();
        assertBody(result, List.of());
        verify(service).getByOwner(1L, BookingState.ALL);
    }

    @ParameterizedTest
    @EnumSource(BookingState.class)
    void getByOwnerForwardsState(BookingState state) throws Exception {
        when(service.getByOwner(1L, state)).thenReturn(ResponseEntity.ok(List.of()));
        MvcResult result = mvc.perform(get("/bookings/owner").header(HEADER, 1).param("state", state.name()))
                .andExpect(status().isOk()).andReturn();
        assertBody(result, List.of());
        verify(service).getByOwner(1L, state);
    }

    @Test
    void getByOwnerRejectsUnknownState() throws Exception {
        mvc.perform(get("/bookings/owner").header(HEADER, 1).param("state", "UNKNOWN"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void requiresUserHeader() throws Exception {
        mvc.perform(get("/bookings")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @MethodSource("invalidBookings")
    void rejectsInvalidDates(BookingRequestDto request) throws Exception {
        mvc.perform(post("/bookings").header(HEADER, 1).contentType(MediaType.APPLICATION_JSON)
                        .content(json(request))).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void requiresApprovedParameter() throws Exception {
        mvc.perform(patch("/bookings/5").header(HEADER, 1)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsNonBooleanApproved() throws Exception {
        mvc.perform(patch("/bookings/5").header(HEADER, 1).param("approved", "not-boolean"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsInvalidUserId() throws Exception {
        mvc.perform(get("/bookings").header(HEADER, -1)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    static Stream<BookingRequestDto> invalidBookings() {
        LocalDateTime now = LocalDateTime.now();
        return Stream.of(new BookingRequestDto(),
                new BookingRequestDto(2L, now.minusDays(1), now.plusDays(1)),
                new BookingRequestDto(2L, now.plusDays(1), now.plusDays(1)),
                new BookingRequestDto(2L, now.plusDays(2), now.plusDays(1)));
    }
}
