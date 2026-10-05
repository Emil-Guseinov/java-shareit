package ru.practicum.shareit.web;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.shareit.common.exception.ErrorHandler;
import ru.practicum.shareit.common.exception.ErrorResponse;
import ru.practicum.shareit.user.UserController;
import ru.practicum.shareit.user.dto.UserDto;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorHandlerTest {
    @Test
    void validationResponseIsSortedAndDoesNotExposeRejectedValues() throws Exception {
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(new UserDto(), "user");
        errors.addError(new FieldError("user", "name", "must not be blank"));
        errors.addError(new FieldError("user", "email", "must be an email"));
        MethodParameter parameter = new MethodParameter(UserController.class.getMethod("create", UserDto.class), 0);
        MethodArgumentNotValidException failure = new MethodArgumentNotValidException(parameter, errors);
        var response = new ErrorHandler().handleValidation(failure);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(new ErrorResponse("email: must be an email; name: must not be blank"), response.getBody());
    }

    @Test
    void springStatusIsNotChangedTo500ByFallback() {
        var response = new ErrorHandler().handleUnexpected(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
    }
}
