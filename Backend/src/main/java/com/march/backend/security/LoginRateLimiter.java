package com.march.backend.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.EstimationProbe;
import io.github.bucket4j.Refill;
import io.github.bucket4j.TimeMeter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Brute-force protection for the login endpoint, backed by Bucket4j.
 * <p>
 * Every (IP address, username) pair gets two in-memory token buckets:
 * <ul>
 *   <li>a <em>failure</em> bucket holding {@code maxFailures} tokens that refill
 *       over {@code window} - a burst of failed logins drains it;</li>
 *   <li>a <em>lockout</em> bucket holding a single token that only comes back
 *       after {@code lockout}. Draining it locks the pair out for that long.</li>
 * </ul>
 * When the failure bucket reaches zero (the {@code maxFailures}-th failure inside
 * the window) the lockout bucket is drained, so the next attempt is refused until
 * the cooldown has elapsed. A successful login clears the pair's state, so a
 * legitimate user who mistypes a few times is never punished once they get in.
 * <p>
 * State is per-instance and in memory; a multi-node deployment would need a
 * shared store (Bucket4j supports JCache/Redis) instead of this map.
 */
@Component
public class LoginRateLimiter {

    /** Above this many tracked pairs, idle ones are swept to bound memory use. */
    private static final int CLEANUP_THRESHOLD = 5_000;

    private final int maxFailures;
    private final Duration window;
    private final Duration lockout;
    private final TimeMeter timeMeter;
    private final Map<String, Counters> counters = new ConcurrentHashMap<>();

    @Autowired
    public LoginRateLimiter(@Value("${app.auth.login.max-failures:5}") int maxFailures,
                            @Value("${app.auth.login.window:PT1M}") Duration window,
                            @Value("${app.auth.login.lockout:PT5M}") Duration lockout) {
        this(maxFailures, window, lockout, TimeMeter.SYSTEM_MILLISECONDS);
    }

    /** Test seam: lets a test drive the buckets from a fake clock. */
    LoginRateLimiter(int maxFailures, Duration window, Duration lockout, TimeMeter timeMeter) {
        this.maxFailures = maxFailures;
        this.window = window;
        this.lockout = lockout;
        this.timeMeter = timeMeter;
    }

    /**
     * Refuse the attempt if the pair is currently locked out.
     *
     * @throws LoginLockedException carrying the seconds until another attempt is allowed
     */
    public void checkAllowed(String ip, String username) {
        Counters counts = counters.get(key(ip, username));
        if (counts == null) {
            return;
        }
        EstimationProbe probe = counts.lockout.estimateAbilityToConsume(1);
        if (!probe.canBeConsumed()) {
            long seconds = Math.max(1L, (probe.getNanosToWaitForRefill() + 999_999_999L) / 1_000_000_000L);
            throw new LoginLockedException(seconds);
        }
    }

    /** Record a failed attempt and arm the lockout once the window is exhausted. */
    public void recordFailure(String ip, String username) {
        sweepIdleEntries();
        Counters counts = counters.computeIfAbsent(key(ip, username), ignored -> newCounters());
        ConsumptionProbe probe = counts.failures.tryConsumeAndReturnRemaining(1);
        if (probe.getRemainingTokens() == 0) {
            // The failure budget for this window is gone: start the cooldown.
            counts.lockout.tryConsume(1);
        }
    }

    /** A successful login clears the pair's failure history. */
    public void recordSuccess(String ip, String username) {
        counters.remove(key(ip, username));
    }

    private Counters newCounters() {
        Bucket failures = Bucket.builder()
                .withCustomTimePrecision(timeMeter)
                .addLimit(Bandwidth.classic(maxFailures, Refill.greedy(maxFailures, window)))
                .build();
        Bucket lockoutBucket = Bucket.builder()
                .withCustomTimePrecision(timeMeter)
                .addLimit(Bandwidth.classic(1, Refill.greedy(1, lockout)))
                .build();
        return new Counters(failures, lockoutBucket);
    }

    /** Drop pairs that have fully refilled, so one-off clients do not linger. */
    private void sweepIdleEntries() {
        if (counters.size() < CLEANUP_THRESHOLD) {
            return;
        }
        counters.entrySet().removeIf(entry -> entry.getValue().isIdle(maxFailures));
    }

    /** Usernames are compared case-insensitively, matching the account lookup. */
    private static String key(String ip, String username) {
        String normalizedIp = ip == null ? "" : ip;
        String normalizedUser = username == null ? "" : username.toLowerCase();
        return normalizedIp + "|" + normalizedUser;
    }

    private record Counters(Bucket failures, Bucket lockout) {

        /** True when neither bucket is holding a penalty any more. */
        boolean isIdle(int failureCapacity) {
            return failures.getAvailableTokens() >= failureCapacity && lockout.getAvailableTokens() >= 1;
        }
    }
}
