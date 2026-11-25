package com.project.hotel_booking.utils;

import org.springframework.stereotype.Component;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/** Coordinates operations that change the same property's room-type inventory. */
@Component
public class InventoryLockManager {
    private final ConcurrentHashMap<InventoryKey, ReentrantLock> inventoryLocks = new ConcurrentHashMap<>();

    public <T> T withLock(UUID propertyId, UUID roomTypeId, Supplier<T> operation) {
        InventoryKey key = new InventoryKey(propertyId, roomTypeId);
        ReentrantLock lock = inventoryLocks.computeIfAbsent(key, ignored -> new ReentrantLock());
        lock.lock();
        try {
            return operation.get();
        } finally {
            lock.unlock();
        }
    }

    private record InventoryKey(UUID propertyId, UUID roomTypeId) {
    }
}
