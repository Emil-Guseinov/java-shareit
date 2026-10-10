package ru.practicum.shareit.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.shareit.common.exception.ErrorHandler;
import ru.practicum.shareit.common.exception.ErrorResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ErrorHandlerTest {
    @Test
    void unexpectedFailureDoesNotExposeInternalMessage() {
        var response = new ErrorHandler().handleUnexpected(new IllegalStateException("secret SQL"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(new ErrorResponse("Внутренняя ошибка сервера"), response.getBody());
    }

    @Test
    void standardSpringHandlerPreservesHttpStatus() throws Exception {
        var response = new ErrorHandler().handleException(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS),
                new ServletWebRequest(new MockHttpServletRequest()));
        assertNotNull(response);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
    }
}
