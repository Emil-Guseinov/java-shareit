package ru.practicum.shareit;

import org.junit.jupiter.api.Test;
import ru.practicum.shareit.support.AbstractIntegrationTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ShareItTests extends AbstractIntegrationTest {
    @Test
    void contextLoads() {
        assertNotNull(entityManager);
        assertNotNull(mvc);
    }
}
