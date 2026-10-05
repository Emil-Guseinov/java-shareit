package ru.practicum.shareit.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import ru.practicum.shareit.user.UserController;
import ru.practicum.shareit.user.dto.UserDto;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorHandlerTest {
    @Test
    void validationMessagesAreSortedAndDoNotExposeRejectedValues() throws Exception {
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(new UserDto(), "user");
        errors.addError(new FieldError("user", "name", "private name", false, null, null, "must not be blank"));
        errors.addError(new FieldError("user", "email", "private email", false, null, null, "must be an email"));
        MethodParameter parameter = new MethodParameter(UserController.class.getMethod("create", UserDto.class), 0);
        var response = new ErrorHandler().handleValidation(new MethodArgumentNotValidException(parameter, errors));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(new ErrorResponse("must be an email; must not be blank"), response.getBody());
    }
}
