package com.project.hotel_booking.utils;

import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/** Serializes concurrent payment requests that carry the same idempotency key. */
@Component
public class IdempotencyLockManager {
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public <T> T withLock(String idempotencyKey, Supplier<T> operation) {
        ReentrantLock lock = locks.computeIfAbsent(idempotencyKey, ignored -> new ReentrantLock());
        lock.lock();
        try {
            return operation.get();
        } finally {
            lock.unlock();
        }
    }
}
