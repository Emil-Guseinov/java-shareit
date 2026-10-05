package ru.practicum.shareit.request;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.RequestItemDto;
import ru.practicum.shareit.request.service.ItemRequestService;
import ru.practicum.shareit.support.ControllerTestSupport;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemRequestController.class)
class ItemRequestControllerTest extends ControllerTestSupport {
    @MockBean
    private ItemRequestService service;

    @Test
    void createReturnsRequest() throws Exception {
        ItemRequestCreateDto request = new ItemRequestCreateDto("Нужна дрель");
        ItemRequestDto expected = new ItemRequestDto(3L, "Нужна дрель", LocalDateTime.of(2030, 1, 15, 12, 0),
                List.of(new RequestItemDto(2L, "Дрель", 8L)));
        when(service.create(eq(1L), any(ItemRequestCreateDto.class))).thenReturn(expected);
        MvcResult result = mvc.perform(post("/requests").header(HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isOk()).andReturn();
        assertBody(result, expected);
        verify(service).create(eq(1L), refEq(request));
    }

    @Test
    void getOwnReturnsRequestsWithAnswers() throws Exception {
        ItemRequestDto expected = new ItemRequestDto(3L, "Нужна дрель", LocalDateTime.of(2030, 1, 15, 12, 0),
                List.of(new RequestItemDto(2L, "Дрель", 8L)));
        when(service.getOwn(1L)).thenReturn(List.of(expected));
        MvcResult result = mvc.perform(get("/requests").header(HEADER, 1)).andExpect(status().isOk()).andReturn();
        assertBody(result, List.of(expected));
        verify(service).getOwn(1L);
    }

    @Test
    void getOthersReturnsRequestsWithAnswers() throws Exception {
        ItemRequestDto expected = new ItemRequestDto(3L, "Нужна дрель", LocalDateTime.of(2030, 1, 15, 12, 0),
                List.of(new RequestItemDto(2L, "Дрель", 8L)));
        when(service.getOthers(1L)).thenReturn(List.of(expected));
        MvcResult result = mvc.perform(get("/requests/all").header(HEADER, 1)).andExpect(status().isOk()).andReturn();
        assertBody(result, List.of(expected));
        verify(service).getOthers(1L);
    }

    @Test
    void getByIdReturnsRequestWithAnswers() throws Exception {
        ItemRequestDto expected = new ItemRequestDto(3L, "Нужна дрель", LocalDateTime.of(2030, 1, 15, 12, 0),
                List.of(new RequestItemDto(2L, "Дрель", 8L)));
        when(service.getById(1L, 3L)).thenReturn(expected);
        MvcResult result = mvc.perform(get("/requests/3").header(HEADER, 1)).andExpect(status().isOk()).andReturn();
        assertBody(result, expected);
        verify(service).getById(1L, 3L);
    }

    @Test
    void requiresHeaderForOwn() throws Exception {
        mvc.perform(get("/requests")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void requiresHeaderForOthers() throws Exception {
        mvc.perform(get("/requests/all")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
