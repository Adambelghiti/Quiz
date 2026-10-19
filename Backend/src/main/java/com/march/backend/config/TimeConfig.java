package com.march.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Central clock so date-based features can be tested with a fixed time. */
@Configuration
public class TimeConfig {

    @Bean
    public Clock appClock() {
        return Clock.systemUTC();
    }
}
