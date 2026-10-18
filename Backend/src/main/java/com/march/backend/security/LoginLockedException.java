package com.march.backend.security;

/**
 * Thrown when an (IP address, username) pair has failed to authenticate too many
 * times and is temporarily locked out. It is translated to HTTP {@code 429 Too
 * Many Requests} with a {@code Retry-After} header so the client knows when it
 * may try again.
 */
public class LoginLockedException extends RuntimeException {

    private final long retryAfterSeconds;

    public LoginLockedException(long retryAfterSeconds) {
        super("Too many failed login attempts. Try again in " + retryAfterSeconds + " seconds.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /** Whole seconds the caller should wait before another attempt is accepted. */
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
