package ru.practicum.shareit.booking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import ru.practicum.shareit.booking.dto.BookerDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingItemDto;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.common.exception.BadRequestException;
import ru.practicum.shareit.common.exception.ErrorResponse;
import ru.practicum.shareit.support.ControllerTestSupport;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
class BookingControllerTest extends ControllerTestSupport {
    @MockBean
    private BookingService service;

    @Test
    void createForwardsBooking() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        BookingRequestDto request = new BookingRequestDto(2L, start, start.plusDays(1));
        BookingDto expected = new BookingDto(5L, start, start.plusDays(1), BookingStatus.WAITING,
                new BookingItemDto(2L, "Дрель"), new BookerDto(1L));
        when(service.create(eq(1L), any(BookingRequestDto.class))).thenReturn(expected);
        MvcResult result = mvc.perform(post("/bookings").header(HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isOk()).andReturn();
        assertBody(result, expected);
        verify(service).create(eq(1L), refEq(request));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void approveForwardsDecision(boolean approved) throws Exception {
        LocalDateTime start = LocalDateTime.of(2030, 1, 15, 12, 0);
        BookingDto expected = new BookingDto(5L, start, start.plusDays(1),
                approved ? BookingStatus.APPROVED : BookingStatus.REJECTED,
                new BookingItemDto(2L, "Дрель"), new BookerDto(1L));
        when(service.approve(1L, 5L, approved)).thenReturn(expected);
        MvcResult result = mvc.perform(patch("/bookings/5").header(HEADER, 1).param("approved", String.valueOf(approved)))
                .andExpect(status().isOk()).andReturn();
        assertBody(result, expected);
        verify(service).approve(1L, 5L, approved);
    }

    @Test
    void getByIdReturnsBooking() throws Exception {
        LocalDateTime start = LocalDateTime.of(2030, 1, 15, 12, 0);
        BookingDto expected = new BookingDto(5L, start, start.plusDays(1), BookingStatus.WAITING,
                new BookingItemDto(2L, "Дрель"), new BookerDto(1L));
        when(service.getById(1L, 5L)).thenReturn(expected);
        MvcResult result = mvc.perform(get("/bookings/5").header(HEADER, 1)).andExpect(status().isOk()).andReturn();
        assertBody(result, expected);
        verify(service).getById(1L, 5L);
    }

    @Test
    void getByBookerUsesDefaultState() throws Exception {
        when(service.getByBooker(1L, BookingState.ALL)).thenReturn(List.of());
        MvcResult result = mvc.perform(get("/bookings").header(HEADER, 1)).andExpect(status().isOk()).andReturn();
        assertBody(result, List.of());
        verify(service).getByBooker(1L, BookingState.ALL);
    }

    @ParameterizedTest
    @EnumSource(BookingState.class)
    void getByBookerForwardsState(BookingState state) throws Exception {
        when(service.getByBooker(1L, state)).thenReturn(List.of());
        MvcResult result = mvc.perform(get("/bookings").header(HEADER, 1).param("state", state.name()))
                .andExpect(status().isOk()).andReturn();
        assertBody(result, List.of());
        verify(service).getByBooker(1L, state);
    }

    @Test
    void getByOwnerUsesDefaultState() throws Exception {
        when(service.getByOwner(1L, BookingState.ALL)).thenReturn(List.of());
        MvcResult result = mvc.perform(get("/bookings/owner").header(HEADER, 1)).andExpect(status().isOk()).andReturn();
        assertBody(result, List.of());
        verify(service).getByOwner(1L, BookingState.ALL);
    }

    @ParameterizedTest
    @EnumSource(BookingState.class)
    void getByOwnerForwardsState(BookingState state) throws Exception {
        when(service.getByOwner(1L, state)).thenReturn(List.of());
        MvcResult result = mvc.perform(get("/bookings/owner").header(HEADER, 1).param("state", state.name()))
                .andExpect(status().isOk()).andReturn();
        assertBody(result, List.of());
        verify(service).getByOwner(1L, state);
    }

    @Test
    void requiresUserHeader() throws Exception {
        mvc.perform(get("/bookings")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void returnsBadRequestForDomainFailure() throws Exception {
        when(service.approve(1L, 5L, true)).thenThrow(new BadRequestException("Неверное состояние"));
        MvcResult result = mvc.perform(patch("/bookings/5").header(HEADER, 1).param("approved", "true"))
                .andExpect(status().isBadRequest()).andReturn();
        assertBody(result, new ErrorResponse("Неверное состояние"));
    }
}
