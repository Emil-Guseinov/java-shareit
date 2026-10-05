package ru.practicum.shareit.item;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MvcResult;
import ru.practicum.shareit.item.dto.CommentRequestDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemRequestDto;
import ru.practicum.shareit.support.ControllerTestSupport;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemController.class)
class ItemControllerTest extends ControllerTestSupport {
    @MockBean
    private ItemClient service;

    @Test
    void createReturnsItemWithRequestId() throws Exception {
        ItemRequestDto request = new ItemRequestDto("Дрель", "Описание", true, 3L);
        ItemDto expected = new ItemDto(2L, "Дрель", "Описание", true, 3L);
        when(service.create(eq(1L), any(ItemRequestDto.class))).thenReturn(ResponseEntity.ok(expected));
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
        when(service.update(eq(1L), eq(2L), any(ItemRequestDto.class))).thenReturn(ResponseEntity.ok(expected));
        MvcResult result = mvc.perform(patch("/items/2").header(HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isOk()).andReturn();
        assertBody(result, expected);
        verify(service).update(eq(1L), eq(2L), refEq(request));
    }

    @Test
    void getByIdReturnsItem() throws Exception {
        ItemDto item = new ItemDto(2L, "Дрель", "Описание", true, 3L);
        when(service.getById(1L, 2L)).thenReturn(ResponseEntity.ok(item));
        MvcResult result = mvc.perform(get("/items/2").header(HEADER, 1)).andExpect(status().isOk()).andReturn();
        assertBody(result, item);
        verify(service).getById(1L, 2L);
    }

    @Test
    void getByOwnerReturnsItems() throws Exception {
        ItemDto item = new ItemDto(2L, "Дрель", "Описание", true, 3L);
        when(service.getByOwner(1L)).thenReturn(ResponseEntity.ok(List.of(item)));
        MvcResult result = mvc.perform(get("/items").header(HEADER, 1)).andExpect(status().isOk()).andReturn();
        assertBody(result, List.of(item));
        verify(service).getByOwner(1L);
    }

    @Test
    void searchForwardsText() throws Exception {
        ItemDto item = new ItemDto(2L, "Дрель", "Описание", true, 3L);
        when(service.search(1L, "дрель + & %")).thenReturn(ResponseEntity.ok(List.of(item)));
        MvcResult result = mvc.perform(get("/items/search").header(HEADER, 1).param("text", "дрель + & %"))
                .andExpect(status().isOk()).andReturn();
        assertBody(result, List.of(item));
        verify(service).search(1L, "дрель + & %");
    }

    @Test
    void addCommentReturnsComment() throws Exception {
        CommentRequestDto request = new CommentRequestDto("Отзыв");
        Map<String, Object> expected = Map.of("id", 4L, "text", "Отзыв", "authorName", "Имя");
        when(service.addComment(eq(1L), eq(2L), any(CommentRequestDto.class)))
                .thenReturn(ResponseEntity.ok(expected));
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

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t\n"})
    void blankSearchReturnsEmptyListWithoutCallingServer(String text) throws Exception {
        MvcResult result = mvc.perform(get("/items/search").header(HEADER, 1).param("text", text))
                .andExpect(status().isOk()).andReturn();
        assertBody(result, List.of());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @MethodSource("invalidItems")
    void rejectsInvalidCreate(ItemRequestDto request) throws Exception {
        mvc.perform(post("/items").header(HEADER, 1).contentType(MediaType.APPLICATION_JSON)
                        .content(json(request))).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsBlankNamePatch() throws Exception {
        mvc.perform(patch("/items/2").header(HEADER, 1).contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ItemRequestDto("   ", null, null))))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsBlankComment() throws Exception {
        mvc.perform(post("/items/2/comment").header(HEADER, 1).contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CommentRequestDto("   ")))).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsInvalidHeader() throws Exception {
        mvc.perform(get("/items").header(HEADER, -1)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsNonNumericItemId() throws Exception {
        mvc.perform(get("/items/abc").header(HEADER, 1)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void doesNotForwardClientControlledItemId() throws Exception {
        Map<String, Object> body = Map.of("id", 999L, "ownerId", 99L, "name", "Дрель",
                "description", "Описание", "available", true);
        when(service.create(eq(1L), any(ItemRequestDto.class)))
                .thenReturn(ResponseEntity.ok(new ItemDto(2L, "Дрель", "Описание", true)));
        mvc.perform(post("/items").header(HEADER, 1).contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isOk());
        verify(service).create(eq(1L), refEq(new ItemRequestDto("Дрель", "Описание", true)));
    }

    static Stream<ItemRequestDto> invalidItems() {
        return Stream.of(new ItemRequestDto(), new ItemRequestDto("", "Описание", true),
                new ItemRequestDto("Дрель", "   ", true), new ItemRequestDto("Дрель", "Описание", null),
                new ItemRequestDto("Дрель", "Описание", true, 0L));
    }
}
