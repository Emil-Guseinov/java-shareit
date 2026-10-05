package ru.practicum.shareit.booking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.common.exception.BadRequestException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.support.AbstractIntegrationTest;
import ru.practicum.shareit.user.User;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class BookingConcurrencyTest extends AbstractIntegrationTest {
    @Autowired
    private BookingService service;

    @Test
    void shouldApproveOnlyOneOfTwoConcurrentOverlappingRequests() throws Exception {
        User owner = user("Владелец");
        User booker = user("Арендатор");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Item item = item(owner, "Дрель", true);
            Booking first = booking(item, booker, NOW.plusDays(1), NOW.plusDays(3), BookingStatus.WAITING);
            Booking second = booking(item, booker, NOW.plusDays(2), NOW.plusDays(4), BookingStatus.WAITING);
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch go = new CountDownLatch(1);
            Callable<Boolean> firstApproval = approval(owner.getId(), first.getId(), ready, go);
            Callable<Boolean> secondApproval = approval(owner.getId(), second.getId(), ready, go);
            Future<Boolean> firstResult = executor.submit(firstApproval);
            Future<Boolean> secondResult = executor.submit(secondApproval);
            assertTrue(ready.await(10, TimeUnit.SECONDS), "Оба потока должны начать проверку");
            go.countDown();
            int successes = (firstResult.get(20, TimeUnit.SECONDS) ? 1 : 0)
                    + (secondResult.get(20, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, successes);
            List<Booking> result = bookings.findAllById(List.of(first.getId(), second.getId()));
            assertEquals(1L, result.stream().filter(b -> b.getStatus() == BookingStatus.APPROVED).count());
            assertEquals(1L, result.stream().filter(b -> b.getStatus() == BookingStatus.WAITING).count());
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
            users.deleteById(owner.getId());
            users.deleteById(booker.getId());
        }
    }

    private Callable<Boolean> approval(long ownerId, long bookingId, CountDownLatch ready, CountDownLatch go) {
        return () -> {
            ready.countDown();
            if (!go.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Не получен сигнал начала проверки");
            }
            try {
                service.approve(ownerId, bookingId, true);
                return true;
            } catch (BadRequestException exception) {
                return false;
            }
        };
    }
}
