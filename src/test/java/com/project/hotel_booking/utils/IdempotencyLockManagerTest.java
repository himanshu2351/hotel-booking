package com.project.hotel_booking.utils;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyLockManagerTest {
    private final IdempotencyLockManager lockManager = new IdempotencyLockManager();

    @Test
    void serializesOperationsForAKeyAndReturnsTheirResults() {
        AtomicInteger calls = new AtomicInteger();

        int firstResult = lockManager.withLock("same-key", calls::incrementAndGet);
        int secondResult = lockManager.withLock("same-key", calls::incrementAndGet);

        assertThat(firstResult).isEqualTo(1);
        assertThat(secondResult).isEqualTo(2);
        assertThat(calls).hasValue(2);
    }
}
