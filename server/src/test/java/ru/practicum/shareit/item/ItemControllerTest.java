package ru.practicum.shareit.item;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import ru.practicum.shareit.common.exception.ErrorResponse;
import ru.practicum.shareit.common.exception.ForbiddenException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.CommentRequestDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemRequestDto;
import ru.practicum.shareit.item.dto.ItemResponseDto;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.support.ControllerTestSupport;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemController.class)
class ItemControllerTest extends ControllerTestSupport {
    @MockBean
    private ItemService service;

    @Test
    void createReturnsItemWithRequestId() throws Exception {
        ItemRequestDto request = new ItemRequestDto("Дрель", "Описание", true, 3L);
        ItemDto expected = new ItemDto(2L, "Дрель", "Описание", true, 3L);
        when(service.create(eq(1L), any(ItemRequestDto.class))).thenReturn(expected);
        MvcResult result = mvc.perform(post("/items").header(HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isOk()).andReturn();
        assertBody(result, expected);
        verify(service).create(eq(1L), refEq(request));
    }

    @Test
    void updateKeepsFalseAndOmittedFields() throws Exception {
        ItemRequestDto request = new ItemRequestDto(null, null, false);
        ItemDto expected = new ItemDto(2L, "Дрель", "Описание", false, 3L);
        when(service.update(eq(1L), eq(2L), any(ItemRequestDto.class))).thenReturn(expected);
        MvcResult result = mvc.perform(patch("/items/2").header(HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isOk()).andReturn();
        assertBody(result, expected);
        verify(service).update(eq(1L), eq(2L), refEq(request));
    }

    @Test
    void getByIdReturnsItem() throws Exception {
        ItemResponseDto item = new ItemResponseDto(2L, "Дрель", "Описание", true, null, null, List.of(), 3L);
        when(service.getById(1L, 2L)).thenReturn(item);
        MvcResult result = mvc.perform(get("/items/2").header(HEADER, 1)).andExpect(status().isOk()).andReturn();
        assertBody(result, item);
        verify(service).getById(1L, 2L);
    }

    @Test
    void getByOwnerReturnsItems() throws Exception {
        ItemResponseDto item = new ItemResponseDto(2L, "Дрель", "Описание", true, null, null, List.of(), 3L);
        when(service.getByOwner(1L)).thenReturn(List.of(item));
        MvcResult result = mvc.perform(get("/items").header(HEADER, 1)).andExpect(status().isOk()).andReturn();
        assertBody(result, List.of(item));
        verify(service).getByOwner(1L);
    }

    @Test
    void searchForwardsText() throws Exception {
        ItemDto item = new ItemDto(2L, "Дрель", "Описание", true, 3L);
        when(service.search("дрель + & %")).thenReturn(List.of(item));
        MvcResult result = mvc.perform(get("/items/search").header(HEADER, 1).param("text", "дрель + & %"))
                .andExpect(status().isOk()).andReturn();
        assertBody(result, List.of(item));
        verify(service).search("дрель + & %");
    }

    @Test
    void addCommentReturnsComment() throws Exception {
        CommentRequestDto request = new CommentRequestDto("Отзыв");
        CommentDto expected = new CommentDto(4L, "Отзыв", "Имя", LocalDateTime.of(2030, 1, 15, 12, 0));
        when(service.addComment(eq(1L), eq(2L), any(CommentRequestDto.class)))
                .thenReturn(expected);
        MvcResult result = mvc.perform(post("/items/2/comment").header(HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isOk()).andReturn();
        assertBody(result, expected);
        verify(service).addComment(eq(1L), eq(2L), refEq(request));
    }

    @Test
    void requiresUserHeader() throws Exception {
        mvc.perform(get("/items")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void requiresSearchText() throws Exception {
        mvc.perform(get("/items/search").header(HEADER, 1)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void returnsForbidden() throws Exception {
        when(service.getById(1L, 2L)).thenThrow(new ForbiddenException("Нет доступа"));
        MvcResult result = mvc.perform(get("/items/2").header(HEADER, 1)).andExpect(status().isForbidden()).andReturn();
        assertBody(result, new ErrorResponse("Нет доступа"));
    }

    @Test
    void inputDoesNotCarryClientId() throws Exception {
        Map<String, Object> body = Map.of("id", 999L, "ownerId", 99L, "name", "Дрель",
                "description", "Описание", "available", true);
        when(service.create(eq(1L), any(ItemRequestDto.class))).thenReturn(new ItemDto(2L, "Дрель", "Описание", true));
        mvc.perform(post("/items").header(HEADER, 1).contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isOk());
        verify(service).create(eq(1L), refEq(new ItemRequestDto("Дрель", "Описание", true)));
    }
}
