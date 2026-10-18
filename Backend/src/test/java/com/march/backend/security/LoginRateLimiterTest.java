package com.march.backend.security;

import io.github.bucket4j.TimeMeter;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The login lockout: five failures inside a minute lock a pair out for five
 * minutes. A fake clock keeps the tests instant while still exercising the real
 * Bucket4j refill maths.
 */
class LoginRateLimiterTest {

    private static final String IP = "203.0.113.7";
    private static final String USER = "alice";

    private final FakeClock clock = new FakeClock();
    private final LoginRateLimiter limiter =
            new LoginRateLimiter(5, Duration.ofMinutes(1), Duration.ofMinutes(5), clock);

    @Test
    void fiveFailuresAreAllowedButTheSixthAttemptIsLocked() {
        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> limiter.checkAllowed(IP, USER));
            limiter.recordFailure(IP, USER);
        }

        LoginLockedException ex = assertThrows(LoginLockedException.class,
                () -> limiter.checkAllowed(IP, USER));
        assertTrue(ex.getRetryAfterSeconds() > 0 && ex.getRetryAfterSeconds() <= 300,
                "the lockout should last at most five minutes, was " + ex.getRetryAfterSeconds() + "s");
    }

    @Test
    void theLockoutEndsAfterFiveMinutes() {
        lockOut(IP, USER);
        assertThrows(LoginLockedException.class, () -> limiter.checkAllowed(IP, USER));

        clock.advance(Duration.ofMinutes(5).plusSeconds(1));

        assertDoesNotThrow(() -> limiter.checkAllowed(IP, USER));
    }

    @Test
    void failuresSpreadBeyondTheWindowDoNotLock() {
        // One failure every 20 seconds is under the five-per-minute budget.
        for (int i = 0; i < 10; i++) {
            limiter.recordFailure(IP, USER);
            clock.advance(Duration.ofSeconds(20));
        }

        assertDoesNotThrow(() -> limiter.checkAllowed(IP, USER));
    }

    @Test
    void aSuccessfulLoginClearsTheFailures() {
        for (int i = 0; i < 4; i++) {
            limiter.recordFailure(IP, USER);
        }
        limiter.recordSuccess(IP, USER);

        for (int i = 0; i < 4; i++) {
            assertDoesNotThrow(() -> limiter.checkAllowed(IP, USER));
            limiter.recordFailure(IP, USER);
        }
    }

    @Test
    void differentUsernamesAndAddressesAreTrackedSeparately() {
        lockOut(IP, USER);

        assertDoesNotThrow(() -> limiter.checkAllowed(IP, "bob"));
        assertDoesNotThrow(() -> limiter.checkAllowed("198.51.100.9", USER));
    }

    @Test
    void usernamesAreCaseInsensitive() {
        lockOut(IP, "Alice");

        assertThrows(LoginLockedException.class, () -> limiter.checkAllowed(IP, "alice"));
    }

    private void lockOut(String ip, String username) {
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure(ip, username);
        }
    }

    /** A mutable, monotonic clock so lockout expiry can be tested instantly. */
    private static final class FakeClock implements TimeMeter {

        private long nanos = 1_000_000_000L;

        @Override
        public long currentTimeNanos() {
            return nanos;
        }

        @Override
        public boolean isWallClockBased() {
            return true;
        }

        void advance(Duration duration) {
            nanos += duration.toNanos();
        }
    }
}
