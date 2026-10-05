package ru.practicum.shareit.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.item.dto.CommentRequestDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.validation.Create;
import ru.practicum.shareit.validation.Update;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@JsonTest
class GatewayJsonTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2030, 1, 15, 12, 0);
    private static final ValidatorFactory VALIDATORS = Validation.byDefaultProvider().configure()
            .clockProvider(() -> Clock.fixed(Instant.parse("2030-01-15T12:00:00Z"), ZoneOffset.UTC))
            .buildValidatorFactory();
    private final Validator validator = VALIDATORS.getValidator();
    @Autowired
    private JacksonTester<BookingRequestDto> bookingJson;
    @Autowired
    private JacksonTester<ItemDto> itemJson;
    @Autowired
    private JacksonTester<UserDto> userJson;
    @Autowired
    private JacksonTester<ItemRequestCreateDto> requestJson;
    @Autowired
    private ObjectMapper mapper;

    @AfterAll
    static void closeValidationFactory() {
        VALIDATORS.close();
    }

    @Test
    void bookingDatesAreIsoStringsAndDeserializeWithoutTimezone() throws Exception {
        BookingRequestDto dto = new BookingRequestDto(4L, NOW.plusHours(1), NOW.plusHours(2));
        assertThat(bookingJson.write(dto)).extractingJsonPathStringValue("$.start")
                .isEqualTo("2030-01-15T13:00:00");
        assertThat(bookingJson.write(dto)).extractingJsonPathStringValue("$.end")
                .isEqualTo("2030-01-15T14:00:00");
        BookingRequestDto restored = bookingJson.parseObject(bookingJson.write(dto).getJson());
        assertThat(restored.getStart()).isEqualTo(dto.getStart());
        assertThat(restored.getItemId()).isEqualTo(4L);
        assertTrue(validator.validate(restored).isEmpty());
    }

    @Test
    void rejectsMalformedDateJson() {
        assertThrows(Exception.class, () -> bookingJson.parseObject(
                "{\"itemId\":1,\"start\":\"not-a-date\",\"end\":\"2030-01-16T12:00:00\"}"));
    }

    @Test
    void validatesMissingDatesAndItemIdExplicitlyAfterJsonParsing() throws Exception {
        assertFalse(validator.validate(bookingJson.parseObject("{}")).isEmpty());
        assertFalse(validator.validate(new BookingRequestDto(1L, null, NOW.plusHours(1))).isEmpty());
        assertFalse(validator.validate(new BookingRequestDto(1L, NOW.plusHours(1), null)).isEmpty());
        assertFalse(validator.validate(new BookingRequestDto(null, NOW.plusHours(1), NOW.plusHours(2))).isEmpty());
        assertFalse(validator.validate(new BookingRequestDto(0L, NOW.plusHours(1), NOW.plusHours(2))).isEmpty());
    }

    @Test
    void datesMustStartStrictlyInFutureAndEndAfterStart() {
        assertFalse(validator.validate(new BookingRequestDto(1L, NOW.minusSeconds(1), NOW.plusHours(1))).isEmpty());
        assertFalse(validator.validate(new BookingRequestDto(1L, NOW, NOW.plusHours(1))).isEmpty());
        assertFalse(validator.validate(new BookingRequestDto(1L, NOW.plusHours(1), NOW.plusHours(1))).isEmpty());
        assertFalse(validator.validate(new BookingRequestDto(1L, NOW.plusHours(2), NOW.plusHours(1))).isEmpty());
        assertTrue(validator.validate(new BookingRequestDto(1L, NOW.plusSeconds(1), NOW.plusHours(1))).isEmpty());
    }

    @Test
    void optionalRequestIdAndPartialPatchSurviveJsonRoundTrip() throws Exception {
        ItemDto noRequest = itemJson.parseObject("{\"name\":\"Дрель\",\"description\":\"Описание\",\"available\":true}");
        assertNull(noRequest.getRequestId());
        assertTrue(validator.validate(noRequest, Create.class).isEmpty());
        ItemDto withRequest = itemJson.parseObject(
                "{\"name\":\"Дрель\",\"description\":\"Описание\",\"available\":true,\"requestId\":7}");
        assertThat(itemJson.write(withRequest)).extractingJsonPathNumberValue("$.requestId").isEqualTo(7);
        ItemDto patch = itemJson.parseObject("{\"available\":false}");
        assertThat(patch.getAvailable()).isFalse();
        assertNull(patch.getName());
        assertTrue(validator.validate(patch, Update.class).isEmpty());
        assertFalse(validator.validate(patch, Create.class).isEmpty());
    }

    @Test
    void emptyAndOversizeFieldsAreRejectedWithCorrectValidationGroups() {
        assertFalse(validator.validate(new ItemDto(null, " ", "Описание", true), Create.class).isEmpty());
        assertFalse(validator.validate(new ItemDto(null, "", null, null), Update.class).isEmpty());
        assertFalse(validator.validate(new ItemDto(null, "x".repeat(256), "Описание", true), Create.class).isEmpty());
        assertFalse(validator.validate(new ItemDto(null, "Имя", "x".repeat(2001), true), Create.class).isEmpty());
        assertFalse(validator.validate(new ItemDto(null, "Имя", "Описание", true, -1L), Create.class).isEmpty());
        assertFalse(validator.validate(new CommentRequestDto(" ")).isEmpty());
        assertFalse(validator.validate(new CommentRequestDto("x".repeat(2001))).isEmpty());
        assertFalse(validator.validate(new ItemRequestCreateDto("x".repeat(2001))).isEmpty());
    }

    @Test
    void missingEmailIsAllowedForPatchButEmptyEmailIsNot() throws Exception {
        UserDto patch = userJson.parseObject("{\"name\":\"Новое имя\"}");
        assertNull(patch.getEmail());
        assertTrue(validator.validate(patch, Update.class).isEmpty());
        assertFalse(validator.validate(patch, Create.class).isEmpty());
        assertFalse(validator.validate(new UserDto(null, null, ""), Update.class).isEmpty());
        assertFalse(validator.validate(new UserDto(null, null, " "), Update.class).isEmpty());
        assertTrue(validator.validate(new UserDto(null, "Имя", "a@example.com"), Create.class).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"description\":null}", "{\"description\":\"\"}", "{\"description\":\"   \"}"})
    void requestDescriptionMustBePresentAndNonBlank(String json) throws Exception {
        assertFalse(validator.validate(requestJson.parseObject(json)).isEmpty());
    }

    @Test
    void requestWriteDtoDoesNotAcceptAuthorityFields() throws Exception {
        ItemRequestCreateDto request = requestJson.parseObject(
                "{\"description\":\"Нужен инструмент\",\"id\":99,\"requesterId\":4,\"created\":\"2000-01-01T00:00:00\"}");
        assertTrue(validator.validate(request).isEmpty());
        assertThat(mapper.readTree(requestJson.write(request).getJson()).size()).isEqualTo(1);
        assertThat(request.getDescription()).isEqualTo("Нужен инструмент");
    }
}
