package ru.practicum.shareit.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(FixedClockConfiguration.class)
@Transactional
public abstract class AbstractIntegrationTest {
    protected static final String USER_HEADER = "X-Sharer-User-Id";
    protected static final LocalDateTime NOW = LocalDateTime.of(2030, 1, 15, 12, 0);

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected UserRepository users;
    @Autowired
    protected ItemRepository items;
    @Autowired
    protected BookingRepository bookings;
    @Autowired
    protected CommentRepository comments;
    @Autowired
    protected EntityManager entityManager;

    protected User user(String name) {
        return users.saveAndFlush(new User(null, name, UUID.randomUUID() + "@example.com"));
    }

    protected Item item(User owner, String name, boolean available) {
        return items.saveAndFlush(new Item(null, name, "Описание " + name, available, owner, null));
    }

    protected Booking booking(Item item, User booker, LocalDateTime start, LocalDateTime end,
                              BookingStatus status) {
        return bookings.saveAndFlush(new Booking(null, start, end, item, booker, status));
    }
}
