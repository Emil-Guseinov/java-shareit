package ru.practicum.shareit.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

@TestConfiguration
public class FixedClockConfiguration {
    @Bean
    @Primary
    public Clock testClock() {
        return Clock.fixed(Instant.parse("2030-01-15T12:00:00Z"), ZoneOffset.UTC);
    }
}
