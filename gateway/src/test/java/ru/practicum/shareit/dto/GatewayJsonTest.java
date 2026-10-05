package ru.practicum.shareit.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.item.dto.CommentRequestDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.validation.Create;
import ru.practicum.shareit.validation.Update;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.stream.Stream;

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
    private JacksonTester<ItemRequestDto> itemJson;
    @Autowired
    private JacksonTester<ItemDto> itemResponseJson;
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
        assertEquals(dto.getStart(), restored.getStart());
        assertEquals(dto.getEnd(), restored.getEnd());
        assertEquals(4L, restored.getItemId());
        assertTrue(validator.validate(restored).isEmpty());
    }

    @Test
    void rejectsMalformedDateJson() throws Exception {
        String json = mapper.writeValueAsString(Map.of("itemId", 1, "start", "not-a-date",
                "end", "2030-01-16T12:00:00"));
        assertThrows(JsonProcessingException.class, () -> bookingJson.parseObject(json));
    }

    @ParameterizedTest
    @MethodSource("invalidBookings")
    void validatesDatesAndItemIdAfterParsing(BookingRequestDto dto) throws Exception {
        BookingRequestDto restored = bookingJson.parseObject(bookingJson.write(dto).getJson());
        assertFalse(validator.validate(restored).isEmpty());
    }

    @Test
    void allowsBookingThatStartsStrictlyAfterNow() {
        assertTrue(validator.validate(new BookingRequestDto(1L, NOW.plusSeconds(1), NOW.plusHours(1))).isEmpty());
    }

    @Test
    void optionalRequestIdSurvivesRoundTrip() throws Exception {
        ItemRequestDto noRequest = itemJson.parseObject(mapper.writeValueAsString(
                Map.of("name", "Дрель", "description", "Описание", "available", true)));
        assertNull(noRequest.getRequestId());
        assertTrue(validator.validate(noRequest, Create.class).isEmpty());
        ItemRequestDto withRequest = new ItemRequestDto("Дрель", "Описание", true, 7L);
        ItemRequestDto restored = itemJson.parseObject(itemJson.write(withRequest).getJson());
        assertEquals(7L, restored.getRequestId());
        assertEquals(withRequest.getName(), restored.getName());
        assertTrue(validator.validate(restored, Create.class).isEmpty());
    }

    @Test
    void partialPatchKeepsFalseDistinctFromMissingFields() throws Exception {
        ItemRequestDto patch = itemJson.parseObject(mapper.writeValueAsString(Map.of("available", false)));
        assertFalse(patch.getAvailable());
        assertNull(patch.getName());
        assertNull(patch.getDescription());
        assertNull(patch.getRequestId());
        assertTrue(validator.validate(patch, Update.class).isEmpty());
        assertFalse(validator.validate(patch, Create.class).isEmpty());
    }

    @Test
    void emptyAndOversizeFieldsAreRejectedWithCorrectValidationGroups() {
        assertFalse(validator.validate(new ItemRequestDto(" ", "Описание", true), Create.class).isEmpty());
        assertFalse(validator.validate(new ItemRequestDto("", null, null), Update.class).isEmpty());
        assertFalse(validator.validate(new ItemRequestDto("x".repeat(256), "Описание", true), Create.class).isEmpty());
        assertFalse(validator.validate(new ItemRequestDto("Имя", "x".repeat(2001), true), Create.class).isEmpty());
        assertFalse(validator.validate(new ItemRequestDto("Имя", "Описание", true, -1L), Create.class).isEmpty());
        assertFalse(validator.validate(new CommentRequestDto(" ")).isEmpty());
        assertFalse(validator.validate(new CommentRequestDto("x".repeat(2001))).isEmpty());
        assertFalse(validator.validate(new ItemRequestCreateDto("x".repeat(2001))).isEmpty());
    }

    @Test
    void itemInputDoesNotAcceptIdOrOwner() throws Exception {
        String body = mapper.writeValueAsString(Map.of("id", 99L, "ownerId", 7L,
                "name", "Дрель", "description", "Описание", "available", true));
        ItemRequestDto dto = itemJson.parseObject(body);
        assertThat(itemJson.write(dto)).doesNotHaveJsonPath("$.id");
        assertThat(itemJson.write(dto)).doesNotHaveJsonPath("$.ownerId");
        assertTrue(validator.validate(dto, Create.class).isEmpty());
    }

    @Test
    void responseDtoHasIdWithoutInputValidation() throws Exception {
        ItemDto response = new ItemDto(2L, "Дрель", "Описание", true, 3L);
        ItemDto restored = itemResponseJson.parseObject(itemResponseJson.write(response).getJson());
        assertEquals(response.getId(), restored.getId());
        assertEquals(response.getName(), restored.getName());
        assertEquals(response.getDescription(), restored.getDescription());
        assertEquals(response.getAvailable(), restored.getAvailable());
        assertEquals(response.getRequestId(), restored.getRequestId());
        assertTrue(validator.validate(new ItemDto(), Create.class).isEmpty());
    }

    @Test
    void missingEmailIsAllowedForPatchButEmptyEmailIsNot() throws Exception {
        UserDto patch = userJson.parseObject(mapper.writeValueAsString(Map.of("name", "Новое имя")));
        assertNull(patch.getEmail());
        assertTrue(validator.validate(patch, Update.class).isEmpty());
        assertFalse(validator.validate(patch, Create.class).isEmpty());
        assertFalse(validator.validate(new UserDto(null, null, ""), Update.class).isEmpty());
        assertFalse(validator.validate(new UserDto(null, null, " "), Update.class).isEmpty());
        assertTrue(validator.validate(new UserDto(null, "Имя", "a@example.com"), Create.class).isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void requestDescriptionMustBePresentAndNonBlank(String description) throws Exception {
        ItemRequestCreateDto dto = new ItemRequestCreateDto(description);
        assertFalse(validator.validate(requestJson.parseObject(requestJson.write(dto).getJson())).isEmpty());
    }

    @Test
    void requestWriteDtoDoesNotAcceptAuthorityFields() throws Exception {
        String json = mapper.writeValueAsString(Map.of("description", "Нужен инструмент", "id", 99,
                "requesterId", 4, "created", "2000-01-01T00:00:00"));
        ItemRequestCreateDto request = requestJson.parseObject(json);
        assertTrue(validator.validate(request).isEmpty());
        assertEquals(1, mapper.readTree(requestJson.write(request).getJson()).size());
        assertEquals("Нужен инструмент", request.getDescription());
    }

    static Stream<BookingRequestDto> invalidBookings() {
        return Stream.of(new BookingRequestDto(),
                new BookingRequestDto(1L, null, NOW.plusHours(1)),
                new BookingRequestDto(1L, NOW.plusHours(1), null),
                new BookingRequestDto(null, NOW.plusHours(1), NOW.plusHours(2)),
                new BookingRequestDto(0L, NOW.plusHours(1), NOW.plusHours(2)),
                new BookingRequestDto(1L, NOW.minusSeconds(1), NOW.plusHours(1)),
                new BookingRequestDto(1L, NOW, NOW.plusHours(1)),
                new BookingRequestDto(1L, NOW.plusHours(1), NOW.plusHours(1)),
                new BookingRequestDto(1L, NOW.plusHours(2), NOW.plusHours(1)));
    }
}
