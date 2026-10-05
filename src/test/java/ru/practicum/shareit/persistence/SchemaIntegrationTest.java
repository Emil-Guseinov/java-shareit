package ru.practicum.shareit.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.item.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.support.AbstractIntegrationTest;
import ru.practicum.shareit.user.User;

import javax.sql.DataSource;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private DataSource dataSource;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldExecuteSchemaTwiceWithoutDroppingExistingData() {
        User existing = user("Сохранённый");
        try {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource("schema.sql"));
            populator.execute(dataSource);
            populator.execute(dataSource);
            assertEquals("Сохранённый", users.findById(existing.getId()).orElseThrow().getName());
        } finally {
            users.deleteById(existing.getId());
        }
    }

    @Test
    void shouldEnforceEmailUniquenessInDatabase() {
        User existing = user("Имя");
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO users (name, email) VALUES (?, ?)", "Другой", existing.getEmail()));
    }

    @Test
    void shouldEnforceForeignKeyInDatabase() {
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO items (name, description, available, owner_id) VALUES (?, ?, ?, ?)",
                        "Вещь", "Описание", true, Long.MAX_VALUE));
    }

    @Test
    void shouldEnforcePositiveBookingDurationInDatabase() {
        User booker = user("Арендатор");
        Item item = item(user("Владелец"), "Дрель", true);
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO bookings (item_id, booker_id, start_date, end_date, status) "
                                + "VALUES (?, ?, ?, ?, ?)", item.getId(), booker.getId(), Timestamp.valueOf(NOW),
                        Timestamp.valueOf(NOW), "WAITING"));
    }

    @Test
    void shouldEnforceBookingStatusInDatabase() {
        User booker = user("Арендатор");
        Item item = item(user("Владелец"), "Дрель", true);
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO bookings (item_id, booker_id, start_date, end_date, status) "
                                + "VALUES (?, ?, ?, ?, ?)", item.getId(), booker.getId(), Timestamp.valueOf(NOW),
                        Timestamp.valueOf(NOW.plusDays(1)), "UNKNOWN"));
    }

    @Test
    void shouldCascadeRelatedDataWhenDeletingOwner() {
        User owner = user("Владелец");
        User booker = user("Арендатор");
        Item item = item(owner, "Дрель", true);
        Long bookingId = booking(item, booker, NOW.minusDays(2), NOW.minusDays(1), BookingStatus.APPROVED).getId();
        Long commentId = comments.saveAndFlush(new Comment(null, "Отзыв", item, booker, NOW)).getId();
        users.delete(owner);
        users.flush();
        entityManager.clear();
        assertFalse(items.existsById(item.getId()));
        assertFalse(bookings.existsById(bookingId));
        assertFalse(comments.existsById(commentId));
        assertTrue(users.existsById(booker.getId()));
    }
}
