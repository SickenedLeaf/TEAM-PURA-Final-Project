package com.gamecheck.security;

import com.gamecheck.exception.TooManyLoginAttemptsException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(10);

    private final ConcurrentHashMap<String, Deque<Instant>> failuresByEmail = new ConcurrentHashMap<>();

    public void assertNotLocked(String normalizedEmail) {
        prune(normalizedEmail);
        Deque<Instant> window = failuresByEmail.get(normalizedEmail);
        if (window != null && window.size() >= MAX_ATTEMPTS) {
            throw new TooManyLoginAttemptsException(
                    "Too many failed login attempts. Try again in a few minutes.");
        }
    }

    public void recordFailure(String normalizedEmail) {
        Instant now = Instant.now();
        failuresByEmail
                .computeIfAbsent(normalizedEmail, k -> new ArrayDeque<>())
                .addLast(now);
        prune(normalizedEmail);
    }

    public void clear(String normalizedEmail) {
        failuresByEmail.remove(normalizedEmail);
    }

    private void prune(String normalizedEmail) {
        Deque<Instant> dq = failuresByEmail.get(normalizedEmail);
        if (dq == null) {
            return;
        }
        Instant cutoff = Instant.now().minus(WINDOW);
        while (!dq.isEmpty() && dq.peekFirst().isBefore(cutoff)) {
            dq.pollFirst();
        }
        if (dq.isEmpty()) {
            failuresByEmail.remove(normalizedEmail, dq);
        }
    }
}
